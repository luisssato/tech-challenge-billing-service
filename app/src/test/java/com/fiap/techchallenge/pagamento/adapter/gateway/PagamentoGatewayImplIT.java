package com.fiap.techchallenge.pagamento.adapter.gateway;

import com.fiap.techchallenge.config.DynamoDbConfig;
import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.pagamento.domain.Pagamento;
import com.fiap.techchallenge.pagamento.domain.StatusPagamento;
import com.fiap.techchallenge.sequence.SequenceGenerator;
import com.fiap.techchallenge.support.DynamoDbTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {DynamoDbConfig.class, SequenceGenerator.class, PagamentoGatewayImpl.class})
class PagamentoGatewayImplIT extends DynamoDbTestSupport {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 28, 10, 0);

    @Autowired
    PagamentoGatewayImpl gateway;

    @Test
    void persisteEReconstroiOPagamento() {
        Pagamento criado = Pagamento.criar(7001L, "orc-1", new BigDecimal("400.00"), AGORA);
        criado.registrarCobranca("mp-7001", "000201...", "iVBOR...", AGORA.plusSeconds(1));
        gateway.save(criado);

        Pagamento lido = gateway.findUltimoByOsId(7001L).orElseThrow();
        assertThat(lido.getId()).isNotBlank();
        assertThat(lido.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(lido.getValor()).isEqualByComparingTo("400.00");
        assertThat(lido.getIdTransacaoProvedor()).isEqualTo("mp-7001");
        assertThat(lido.getQrCode()).isEqualTo("000201...");
        assertThat(lido.getQrCodeBase64()).isEqualTo("iVBOR...");
    }

    @Test
    void salvarNovamenteAtualizaOMesmoPagamento() {
        Pagamento salvo = gateway.save(Pagamento.criar(7002L, "orc-2", new BigDecimal("50.00"), AGORA));
        salvo.confirmar("mp-7002", AGORA.plusMinutes(1));
        gateway.save(salvo);

        Pagamento lido = gateway.findUltimoByOsId(7002L).orElseThrow();
        assertThat(lido.getId()).isEqualTo(salvo.getId());
        assertThat(lido.getStatus()).isEqualTo(StatusPagamento.APROVADO);
    }

    @Test
    void devolveOMaisRecenteQuandoHaNovasTentativas() {
        Pagamento antigo = gateway.save(Pagamento.criar(7003L, "orc-3", new BigDecimal("90.00"), AGORA));
        antigo.recusar("mp-7003-a", AGORA.plusMinutes(1));
        gateway.save(antigo);
        Pagamento novo = gateway.save(Pagamento.criar(7003L, "orc-3", new BigDecimal("90.00"), AGORA.plusHours(1)));

        assertThat(gateway.findUltimoByOsId(7003L).orElseThrow().getId()).isEqualTo(novo.getId());
    }

    @Test
    void naoPermiteDuasTransacoesComOMesmoIdDoProvedor() {
        Pagamento a = Pagamento.criar(7004L, "orc-4", new BigDecimal("10.00"), AGORA);
        a.registrarCobranca("mp-dup", null, null, AGORA);
        gateway.save(a);

        Pagamento b = Pagamento.criar(7005L, "orc-5", new BigDecimal("10.00"), AGORA);
        b.registrarCobranca("mp-dup", null, null, AGORA);

        assertThrows(ConflictException.class, () -> gateway.save(b));
    }

    @Test
    void variosPagamentosSemTransacaoConvivemGracasAoIndiceEsparso() {
        gateway.save(Pagamento.criar(7006L, "orc-6", new BigDecimal("10.00"), AGORA));
        gateway.save(Pagamento.criar(7007L, "orc-7", new BigDecimal("10.00"), AGORA));

        assertThat(gateway.findUltimoByOsId(7006L)).isPresent();
        assertThat(gateway.findUltimoByOsId(7007L)).isPresent();
    }

    @Test
    void buscaPorIdTransacaoProvedor() {
        Pagamento p = Pagamento.criar(7008L, "orc-8", new BigDecimal("75.00"), AGORA);
        p.registrarCobranca("mp-busca-123", "000201", "base64", AGORA);
        gateway.save(p);

        assertThat(gateway.findByIdTransacaoProvedor("mp-busca-123")).isPresent()
                .get().extracting(Pagamento::getOsId).isEqualTo(7008L);
        assertThat(gateway.findByIdTransacaoProvedor("inexistente")).isEmpty();
    }

    @Test
    void osSemPagamentoRetornaVazio() {
        assertThat(gateway.findUltimoByOsId(-1L)).isEmpty();
    }
}

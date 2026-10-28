package com.fiap.techchallenge.orcamento.adapter.gateway;

import com.fiap.techchallenge.config.DynamoDbConfig;
import com.fiap.techchallenge.config.JacksonConfig;
import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.orcamento.domain.ItemOrcamento;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.domain.StatusOrcamento;
import com.fiap.techchallenge.orcamento.domain.TipoItem;
import com.fiap.techchallenge.sequence.SequenceGenerator;
import com.fiap.techchallenge.support.DynamoDbTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {DynamoDbConfig.class, JacksonConfig.class, SequenceGenerator.class, OrcamentoGatewayImpl.class})
class OrcamentoGatewayImplIT extends DynamoDbTestSupport {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 28, 10, 0);

    @Autowired
    OrcamentoGatewayImpl gateway;

    private Orcamento novo(Long osId) {
        return Orcamento.gerar(osId, List.of(
                new ItemOrcamento(TipoItem.PECA, 7L, "Pastilha", 2, new BigDecimal("150.50")),
                new ItemOrcamento(TipoItem.SERVICO, 3L, "Troca de pastilhas", 1, new BigDecimal("100.00"))), AGORA);
    }

    @Test
    void persisteEReconstroiOOrcamentoComItensEValores() {
        Orcamento salvo = gateway.save(novo(9001L));

        assertThat(salvo.getId()).isNotBlank();
        Orcamento lido = gateway.findByOsId(9001L).orElseThrow();
        assertThat(lido.getId()).isEqualTo(salvo.getId());
        assertThat(lido.getStatus()).isEqualTo(StatusOrcamento.GERADO);
        assertThat(lido.getItens()).hasSize(2);
        assertThat(lido.getItens().get(0).getValorUnitario()).isEqualByComparingTo("150.50");
        assertThat(lido.getValorTotal()).isEqualByComparingTo("401.00");
        assertThat(lido.getDataExpiracao()).isEqualTo(AGORA.plusDays(Orcamento.VALIDADE_EM_DIAS));
    }

    @Test
    void salvarNovamenteAtualizaOMesmoDocumento() {
        Orcamento salvo = gateway.save(novo(9002L));
        salvo.aprovar(AGORA.plusHours(1));
        gateway.save(salvo);

        Orcamento lido = gateway.findByOsId(9002L).orElseThrow();
        assertThat(lido.getId()).isEqualTo(salvo.getId());
        assertThat(lido.getStatus()).isEqualTo(StatusOrcamento.APROVADO);
    }

    @Test
    void naoPermiteDoisOrcamentosParaAMesmaOs() {
        gateway.save(novo(9003L));

        assertThrows(ConflictException.class, () -> gateway.save(novo(9003L)));
    }

    @Test
    void osSemOrcamentoRetornaVazio() {
        assertThat(gateway.findByOsId(-1L)).isEmpty();
    }
}

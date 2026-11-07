package com.fiap.techchallenge.orcamento.domain;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.exception.ConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OrcamentoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 21, 10, 0);

    private ItemOrcamento peca(int quantidade, String valorUnitario) {
        return new ItemOrcamento(TipoItem.PECA, 1L, "Pastilha de freio", quantidade, new BigDecimal(valorUnitario));
    }

    private ItemOrcamento servico(int quantidade, String valorUnitario) {
        return new ItemOrcamento(TipoItem.SERVICO, 2L, "Troca de pastilha", quantidade, new BigDecimal(valorUnitario));
    }

    private Orcamento orcamentoGerado() {
        return Orcamento.gerar(10L, List.of(peca(2, "50.00"), servico(1, "80.00")), AGORA);
    }

    private Orcamento orcamentoComStatus(StatusOrcamento status) {
        return new Orcamento("abc", 10L, status, List.of(peca(1, "10.00"), servico(1, "10.00")),
                AGORA, AGORA.plusDays(Orcamento.VALIDADE_EM_DIAS), AGORA);
    }

    // gerar()

    @Test
    public void given_pecasEServicos_when_geraOrcamento_then_statusGeradoComValidadeDeCincoDias() {
        Orcamento orcamento = orcamentoGerado();

        assertNull(orcamento.getId());
        assertEquals(10L, orcamento.getOsId());
        assertEquals(StatusOrcamento.GERADO, orcamento.getStatus());
        assertEquals(AGORA, orcamento.getDataCriacao());
        assertEquals(AGORA, orcamento.getDataAtualizacao());
        assertEquals(AGORA.plusDays(5), orcamento.getDataExpiracao());
        assertEquals(2, orcamento.getItens().size());
    }

    @Test
    public void given_pecasEServicos_when_geraOrcamento_then_calculaTotais() {
        Orcamento orcamento = orcamentoGerado();

        assertEquals(new BigDecimal("100.00"), orcamento.getValorTotalPecas());
        assertEquals(new BigDecimal("80.00"), orcamento.getValorTotalServicos());
        assertEquals(new BigDecimal("180.00"), orcamento.getValorTotal());
    }

    @Test
    public void given_osIdNulo_when_geraOrcamento_then_lancaBadRequest() {
        var itens = List.of(peca(1, "10.00"), servico(1, "10.00"));

        assertThrows(BadRequestException.class, () -> Orcamento.gerar(null, itens, AGORA));
    }

    @Test
    public void given_itensNulosOuVazios_when_geraOrcamento_then_lancaBadRequest() {
        assertThrows(BadRequestException.class, () -> Orcamento.gerar(10L, null, AGORA));
        assertThrows(BadRequestException.class, () -> Orcamento.gerar(10L, List.of(), AGORA));
    }

    @Test
    public void given_apenasPecas_when_geraOrcamento_then_lancaBadRequest() {
        var itens = List.of(peca(1, "10.00"));

        assertThrows(BadRequestException.class, () -> Orcamento.gerar(10L, itens, AGORA));
    }

    @Test
    public void given_apenasServicos_when_geraOrcamento_then_lancaBadRequest() {
        var itens = List.of(servico(1, "10.00"));

        assertThrows(BadRequestException.class, () -> Orcamento.gerar(10L, itens, AGORA));
    }

    @Test
    public void given_valorTotalZero_when_geraOrcamento_then_lancaBadRequest() {
        var itens = List.of(peca(1, "0.00"), servico(1, "0.00"));

        assertThrows(BadRequestException.class, () -> Orcamento.gerar(10L, itens, AGORA));
    }

    @Test
    public void given_listaOriginalAlterada_when_geraOrcamento_then_orcamentoNaoMuda() {
        List<ItemOrcamento> itens = new ArrayList<>(List.of(peca(1, "10.00"), servico(1, "10.00")));
        Orcamento orcamento = Orcamento.gerar(10L, itens, AGORA);

        itens.add(peca(5, "100.00"));

        assertEquals(2, orcamento.getItens().size());
        assertEquals(new BigDecimal("20.00"), orcamento.getValorTotal());
        assertThrows(UnsupportedOperationException.class, () -> orcamento.getItens().add(peca(1, "1.00")));
    }

    // estaVencido()

    @Test
    public void given_dentroDaValidade_when_verificaVencimento_then_naoVencido() {
        assertFalse(orcamentoGerado().estaVencido(AGORA.plusDays(1)));
    }

    @Test
    public void given_exatamenteNoLimiteDaValidade_when_verificaVencimento_then_naoVencido() {
        Orcamento orcamento = orcamentoGerado();

        assertFalse(orcamento.estaVencido(orcamento.getDataExpiracao()));
    }

    @Test
    public void given_umSegundoAposAValidade_when_verificaVencimento_then_vencido() {
        Orcamento orcamento = orcamentoGerado();

        assertTrue(orcamento.estaVencido(orcamento.getDataExpiracao().plusSeconds(1)));
    }

    @Test
    public void given_orcamentoAprovado_when_verificaVencimentoAposValidade_then_naoVencido() {
        Orcamento orcamento = orcamentoComStatus(StatusOrcamento.APROVADO);

        assertFalse(orcamento.estaVencido(AGORA.plusDays(30)));
    }

    // aprovar()

    @Test
    public void given_orcamentoGerado_when_aprova_then_statusAprovadoEAtualizaData() {
        Orcamento orcamento = orcamentoGerado();
        LocalDateTime momento = AGORA.plusHours(3);

        orcamento.aprovar(momento);

        assertEquals(StatusOrcamento.APROVADO, orcamento.getStatus());
        assertEquals(momento, orcamento.getDataAtualizacao());
    }

    @Test
    public void given_orcamentoNoLimiteDaValidade_when_aprova_then_aprovado() {
        Orcamento orcamento = orcamentoGerado();

        orcamento.aprovar(orcamento.getDataExpiracao());

        assertEquals(StatusOrcamento.APROVADO, orcamento.getStatus());
    }

    @Test
    public void given_orcamentoVencido_when_aprova_then_lancaConflictSemAlterarStatus() {
        Orcamento orcamento = orcamentoGerado();
        LocalDateTime depoisDaValidade = orcamento.getDataExpiracao().plusSeconds(1);

        assertThrows(ConflictException.class, () -> orcamento.aprovar(depoisDaValidade));

        assertEquals(StatusOrcamento.GERADO, orcamento.getStatus());
        assertEquals(AGORA, orcamento.getDataAtualizacao());
    }

    @Test
    public void given_orcamentoNaoGerado_when_aprova_then_lancaConflict() {
        for (StatusOrcamento status : List.of(StatusOrcamento.APROVADO, StatusOrcamento.REJEITADO, StatusOrcamento.EXPIRADO)) {
            Orcamento orcamento = orcamentoComStatus(status);

            assertThrows(ConflictException.class, () -> orcamento.aprovar(AGORA), "status " + status);
            assertEquals(status, orcamento.getStatus());
        }
    }

    // rejeitar()

    @Test
    public void given_orcamentoGerado_when_rejeita_then_statusRejeitadoEAtualizaData() {
        Orcamento orcamento = orcamentoGerado();
        LocalDateTime momento = AGORA.plusHours(1);

        orcamento.rejeitar(momento);

        assertEquals(StatusOrcamento.REJEITADO, orcamento.getStatus());
        assertEquals(momento, orcamento.getDataAtualizacao());
    }

    @Test
    public void given_orcamentoNaoGerado_when_rejeita_then_lancaConflict() {
        for (StatusOrcamento status : List.of(StatusOrcamento.APROVADO, StatusOrcamento.REJEITADO, StatusOrcamento.EXPIRADO)) {
            Orcamento orcamento = orcamentoComStatus(status);

            assertThrows(ConflictException.class, () -> orcamento.rejeitar(AGORA), "status " + status);
            assertEquals(status, orcamento.getStatus());
        }
    }

    // expirar()

    @Test
    public void given_orcamentoVencido_when_expira_then_statusExpiradoEAtualizaData() {
        Orcamento orcamento = orcamentoGerado();
        LocalDateTime momento = orcamento.getDataExpiracao().plusDays(1);

        orcamento.expirar(momento);

        assertEquals(StatusOrcamento.EXPIRADO, orcamento.getStatus());
        assertEquals(momento, orcamento.getDataAtualizacao());
    }

    @Test
    public void given_orcamentoDentroDaValidade_when_expira_then_lancaConflict() {
        Orcamento orcamento = orcamentoGerado();

        assertThrows(ConflictException.class, () -> orcamento.expirar(orcamento.getDataExpiracao()));

        assertEquals(StatusOrcamento.GERADO, orcamento.getStatus());
    }

    @Test
    public void given_orcamentoNaoGerado_when_expira_then_lancaConflict() {
        Orcamento orcamento = orcamentoComStatus(StatusOrcamento.APROVADO);

        assertThrows(ConflictException.class, () -> orcamento.expirar(AGORA.plusDays(30)));

        assertEquals(StatusOrcamento.APROVADO, orcamento.getStatus());
    }
}

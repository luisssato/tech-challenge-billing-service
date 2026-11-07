package com.fiap.techchallenge.bdd;

import com.fiap.techchallenge.mensageria.usecase.EventoEntradaUseCase;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.domain.StatusOrcamento;
import com.fiap.techchallenge.orcamento.inputdata.ItemDiagnosticoInputData;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoGateway;
import com.fiap.techchallenge.pagamento.domain.Pagamento;
import com.fiap.techchallenge.pagamento.domain.StatusPagamento;
import com.fiap.techchallenge.pagamento.usecase.PagamentoGateway;
import com.fiap.techchallenge.pagamento.usecase.PagamentoUseCase;
import com.fiap.techchallenge.peca.domain.TipoPeca;
import com.fiap.techchallenge.peca.usecase.TipoPecaGateway;
import com.fiap.techchallenge.servico.domain.TipoServico;
import com.fiap.techchallenge.servico.usecase.TipoServicoGateway;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class FaturamentoSteps {

    @Autowired
    private TipoPecaGateway tipoPecaGateway;

    @Autowired
    private TipoServicoGateway tipoServicoGateway;

    @Autowired
    private OrcamentoGateway orcamentoGateway;

    @Autowired
    private PagamentoGateway pagamentoGateway;

    @Autowired
    private EventoEntradaUseCase eventoEntradaUseCase;

    @Autowired
    private PagamentoUseCase pagamentoUseCase;

    @Dado("que existe o tipo de peça com ID {long}, descrição {string} e valor {bigdecimal}")
    public void criarTipoPeca(Long id, String descricao, BigDecimal preco) {
        tipoPecaGateway.save(new TipoPeca(id, descricao, preco));
    }

    @Dado("que existe o tipo de serviço com ID {long}, descrição {string} e valor {bigdecimal}")
    public void criarTipoServico(Long id, String descricao, BigDecimal preco) {
        tipoServicoGateway.save(new TipoServico(id, descricao, preco));
    }

    @Quando("o evento DIAGNOSTICO_CONCLUIDO chega para a OS {long} com {int} unidades da peça {long} e {int} unidade(s) do serviço {long}")
    public void diagnosticoConcluidoChega(Long osId, int qtdPeca, Long pecaId, int qtdServico, Long servicoId) {
        List<ItemDiagnosticoInputData> pecas = new ArrayList<>();
        if (qtdPeca > 0) {
            pecas.add(new ItemDiagnosticoInputData(pecaId, qtdPeca));
        }

        List<ItemDiagnosticoInputData> servicos = new ArrayList<>();
        if (qtdServico > 0) {
            servicos.add(new ItemDiagnosticoInputData(servicoId, qtdServico));
        }

        eventoEntradaUseCase.processarDiagnosticoConcluido(UUID.randomUUID(), osId, pecas, servicos);
    }

    @Então("o orçamento da OS {long} fica {string} com valor total {bigdecimal}")
    public void verificarOrcamento(Long osId, String statusEsperado, BigDecimal valorTotalEsperado) {
        Optional<Orcamento> orcamentoOpt = orcamentoGateway.findByOsId(osId);
        assertThat(orcamentoOpt).isPresent();
        assertThat(orcamentoOpt.get().getStatus()).isEqualTo(StatusOrcamento.valueOf(statusEsperado));
        assertThat(orcamentoOpt.get().getValorTotal()).isEqualByComparingTo(valorTotalEsperado);
    }

    @Quando("o evento ORCAMENTO_APROVADO chega para a OS {long}")
    public void orcamentoAprovadoChega(Long osId) {
        eventoEntradaUseCase.processarOrcamentoAprovado(UUID.randomUUID(), osId);
    }

    @Então("o orçamento da OS {long} fica {string}")
    public void verificarStatusOrcamento(Long osId, String statusEsperado) {
        Optional<Orcamento> orcamentoOpt = orcamentoGateway.findByOsId(osId);
        assertThat(orcamentoOpt).isPresent();
        assertThat(orcamentoOpt.get().getStatus()).isEqualTo(StatusOrcamento.valueOf(statusEsperado));
    }

    @E("a cobrança de pagamento é criada com status {string} e QR Code disponível")
    public void verificarCobrancaCriada(String statusEsperado) {
        Pagamento pagamento = pagamentoGateway.findUltimoByOsId(2001L).orElseThrow();
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.valueOf(statusEsperado));
        assertThat(pagamento.getQrCode()).isNotNull();
        assertThat(pagamento.getIdTransacaoProvedor()).isNotNull();
    }

    @Quando("a notificação de pagamento aprovado é processada para a OS {long}")
    public void notificacaoPagamentoAprovado(Long osId) {
        Pagamento pagamento = pagamentoGateway.findUltimoByOsId(osId).orElseThrow();
        pagamentoUseCase.processarNotificacaoWebhook(pagamento.getIdTransacaoProvedor());
    }

    @Então("o pagamento da OS {long} fica {string}")
    public void verificarStatusPagamento(Long osId, String statusEsperado) {
        Pagamento pagamento = pagamentoGateway.findUltimoByOsId(osId).orElseThrow();
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.valueOf(statusEsperado));
    }

    @Dado("que existe a OS {long} com pagamento {string} no valor de {bigdecimal}")
    public void criarOsComPagamentoAprovado(Long osId, String status, BigDecimal valor) {
        LocalDateTime agora = LocalDateTime.now();
        Pagamento pagamento = Pagamento.criar(osId, "orc-" + osId, valor, agora);
        if ("APROVADO".equalsIgnoreCase(status)) {
            pagamento.confirmar("mp-pago-" + osId, agora);
        }
        pagamentoGateway.save(pagamento);
    }

    @Quando("o evento EXECUCAO_FALHOU chega para a OS {long} com motivo {string}")
    public void execucaoFalhouChega(Long osId, String motivo) {
        eventoEntradaUseCase.processarExecucaoFalhou(UUID.randomUUID(), osId, motivo, "Falha de teste");
    }

    @Quando("o evento OS_CANCELADA chega para a OS {long} com motivo {string}")
    public void osCanceladaChega(Long osId, String motivo) {
        eventoEntradaUseCase.processarOsCancelada(UUID.randomUUID(), osId, motivo);
    }
}


package com.fiap.techchallenge.orcamento.usecase;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.exception.NotFoundException;
import com.fiap.techchallenge.mensageria.domain.TipoEvento;
import com.fiap.techchallenge.mensageria.usecase.EventoSaidaGateway;
import com.fiap.techchallenge.orcamento.domain.ItemOrcamento;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.inputdata.ItemDiagnosticoInputData;
import com.fiap.techchallenge.peca.usecase.TipoPecaGateway;
import com.fiap.techchallenge.servico.usecase.TipoServicoGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class OrcamentoInteractor implements OrcamentoUseCase {

    private final OrcamentoGateway orcamentoGateway;
    private final TipoPecaGateway tipoPecaGateway;
    private final TipoServicoGateway tipoServicoGateway;
    private final EventoSaidaGateway eventoSaidaGateway;
    private final Clock clock;

    public OrcamentoInteractor(OrcamentoGateway orcamentoGateway, TipoPecaGateway tipoPecaGateway,
            TipoServicoGateway tipoServicoGateway, EventoSaidaGateway eventoSaidaGateway, Clock clock) {
        this.orcamentoGateway = orcamentoGateway;
        this.tipoPecaGateway = tipoPecaGateway;
        this.tipoServicoGateway = tipoServicoGateway;
        this.eventoSaidaGateway = eventoSaidaGateway;
        this.clock = clock;
    }

    @Override
    public Optional<Orcamento> getOrcamentoPorOs(Long osId) {
        return this.orcamentoGateway.findByOsId(osId);
    }

    @Override
    public Orcamento gerarOrcamento(Long osId, List<ItemDiagnosticoInputData> pecas,
            List<ItemDiagnosticoInputData> servicos) {
        if (osId == null) {
            throw new BadRequestException("Erro: O orçamento deve estar vinculado a uma Ordem de Serviço!");
        }

        Optional<Orcamento> existente = this.orcamentoGateway.findByOsId(osId);
        if (existente.isPresent()) {
            log.info("Orçamento da OS {} já existe (status {}); geração ignorada.", osId, existente.get().getStatus());
            return existente.get();
        }

        List<ItemOrcamento> itens = new ArrayList<>();
        for (ItemDiagnosticoInputData peca : orVazia(pecas)) {
            var tipoPeca = this.tipoPecaGateway.findById(peca.idTipo())
                    .orElseThrow(
                            () -> new NotFoundException("Tipo de peça de id " + peca.idTipo() + " não encontrado!"));
            itens.add(ItemOrcamento.deTipoPeca(tipoPeca, peca.quantidade()));
        }
        for (ItemDiagnosticoInputData servico : orVazia(servicos)) {
            var tipoServico = this.tipoServicoGateway.findById(servico.idTipo())
                    .orElseThrow(() -> new NotFoundException(
                            "Tipo de serviço de id " + servico.idTipo() + " não encontrado!"));
            itens.add(ItemOrcamento.deTipoServico(tipoServico, servico.quantidade()));
        }

        Orcamento orcamento = Orcamento.gerar(osId, itens, agora());
        return this.orcamentoGateway.save(orcamento);
    }

    @Override
    public Orcamento aprovarOrcamento(Long osId) {
        Orcamento orcamento = buscarOuFalhar(osId);
        LocalDateTime agora = agora();

        if (orcamento.estaVencido(agora)) {
            orcamento.expirar(agora);
            this.orcamentoGateway.save(orcamento);
            throw new ConflictException(
                    "Erro: O orçamento da Ordem de Serviço %d expirou em %s!".formatted(osId,
                            orcamento.getDataExpiracao()));
        }

        orcamento.aprovar(agora);
        return this.orcamentoGateway.save(orcamento);
    }

    @Override
    public Orcamento rejeitarOrcamento(Long osId) {
        Orcamento orcamento = buscarOuFalhar(osId);
        orcamento.rejeitar(agora());
        Orcamento salvo = this.orcamentoGateway.save(orcamento);
        this.eventoSaidaGateway.publicarEvento(TipoEvento.ORCAMENTO_REJEITADO, String.valueOf(osId), Map.of(
                "osId", osId,
                "motivo", "REJEITADO_PELO_CLIENTE"));
        return salvo;
    }

    private Orcamento buscarOuFalhar(Long osId) {
        return this.orcamentoGateway.findByOsId(osId)
                .orElseThrow(() -> {
                    log.warn("Orçamento da OS {} não encontrado!", osId);
                    return new NotFoundException("Orçamento da ordem de serviço de id " + osId + " não encontrado!");
                });
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }

    private static List<ItemDiagnosticoInputData> orVazia(List<ItemDiagnosticoInputData> itens) {
        return itens == null ? List.of() : itens;
    }
}

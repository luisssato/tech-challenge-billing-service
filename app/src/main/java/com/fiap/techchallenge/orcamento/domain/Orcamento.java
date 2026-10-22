package com.fiap.techchallenge.orcamento.domain;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.exception.ConflictException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Orcamento {

    public static final int VALIDADE_EM_DIAS = 5;

    private final String id;
    private final Long osId;
    private StatusOrcamento status;
    private final List<ItemOrcamento> itens;
    private final LocalDateTime dataCriacao;
    private final LocalDateTime dataExpiracao;
    private LocalDateTime dataAtualizacao;

    public Orcamento(String id, Long osId, StatusOrcamento status, List<ItemOrcamento> itens,
                     LocalDateTime dataCriacao, LocalDateTime dataExpiracao, LocalDateTime dataAtualizacao) {
        this.id = id;
        this.osId = osId;
        this.status = status;
        this.itens = List.copyOf(itens);
        this.dataCriacao = dataCriacao;
        this.dataExpiracao = dataExpiracao;
        this.dataAtualizacao = dataAtualizacao;
    }

    /**
     * Gera um orçamento a partir do diagnóstico concluído. Mantém a regra da Fase 3
     * (OrdemDeServico.podeTransicionarParaStatus -> AGUARDANDO_APROVACAO): o orçamento
     * exige peças e serviços cadastrados e valor total maior que zero.
     */
    public static Orcamento gerar(Long osId, List<ItemOrcamento> itens, LocalDateTime agora) {
        if (osId == null) {
            throw new BadRequestException("Erro: O orçamento deve estar vinculado a uma Ordem de Serviço!");
        }

        boolean possuiPecas = itens != null && itens.stream().anyMatch(i -> i.getTipo() == TipoItem.PECA);
        boolean possuiServicos = itens != null && itens.stream().anyMatch(i -> i.getTipo() == TipoItem.SERVICO);
        if (!possuiPecas || !possuiServicos) {
            throw new BadRequestException(
                    "Erro: O orçamento da Ordem de Serviço %d deve possuir peças e serviços cadastrados!".formatted(osId));
        }

        Orcamento orcamento = new Orcamento(null, osId, StatusOrcamento.GERADO, itens,
                agora, agora.plusDays(VALIDADE_EM_DIAS), agora);

        if (orcamento.getValorTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                    "Erro: O valor total do orçamento da Ordem de Serviço %d deve ser maior que zero!".formatted(osId));
        }

        return orcamento;
    }

    public boolean estaVencido(LocalDateTime agora) {
        return this.status == StatusOrcamento.GERADO && agora.isAfter(this.dataExpiracao);
    }

    public void aprovar(LocalDateTime agora) {
        exigirStatusGerado("aprovado");
        if (estaVencido(agora)) {
            throw new ConflictException(
                    "Erro: O orçamento da Ordem de Serviço %d expirou em %s!".formatted(osId, dataExpiracao));
        }
        transicionarPara(StatusOrcamento.APROVADO, agora);
    }

    public void rejeitar(LocalDateTime agora) {
        exigirStatusGerado("rejeitado");
        transicionarPara(StatusOrcamento.REJEITADO, agora);
    }

    public void expirar(LocalDateTime agora) {
        exigirStatusGerado("expirado");
        if (!estaVencido(agora)) {
            throw new ConflictException(
                    "Erro: O orçamento da Ordem de Serviço %d ainda está dentro da validade (%s)!".formatted(osId, dataExpiracao));
        }
        transicionarPara(StatusOrcamento.EXPIRADO, agora);
    }

    public BigDecimal getValorTotalPecas() {
        return somarSubtotais(TipoItem.PECA);
    }

    public BigDecimal getValorTotalServicos() {
        return somarSubtotais(TipoItem.SERVICO);
    }

    public BigDecimal getValorTotal() {
        return getValorTotalPecas().add(getValorTotalServicos());
    }

    private BigDecimal somarSubtotais(TipoItem tipo) {
        return itens.stream()
                .filter(item -> item.getTipo() == tipo)
                .map(ItemOrcamento::getSubtotal)
                .reduce(BigDecimal.valueOf(0L, 2), BigDecimal::add);
    }

    private void exigirStatusGerado(String acao) {
        if (this.status != StatusOrcamento.GERADO) {
            throw new ConflictException(
                    "Erro: O orçamento da Ordem de Serviço %d deve estar no status '%s' para ser %s!"
                            .formatted(osId, StatusOrcamento.GERADO, acao));
        }
    }

    private void transicionarPara(StatusOrcamento novoStatus, LocalDateTime agora) {
        this.status = novoStatus;
        this.dataAtualizacao = agora;
    }

    public String getId() { return id; }
    public Long getOsId() { return osId; }
    public StatusOrcamento getStatus() { return status; }
    public List<ItemOrcamento> getItens() { return itens; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public LocalDateTime getDataExpiracao() { return dataExpiracao; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
}

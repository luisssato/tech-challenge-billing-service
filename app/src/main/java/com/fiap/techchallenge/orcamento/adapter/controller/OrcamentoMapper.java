package com.fiap.techchallenge.orcamento.adapter.controller;

import com.fiap.techchallenge.orcamento.adapter.controller.dto.ItemOrcamentoResponse;
import com.fiap.techchallenge.orcamento.adapter.controller.dto.OrcamentoResponse;
import com.fiap.techchallenge.orcamento.domain.ItemOrcamento;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import org.springframework.stereotype.Component;

@Component
public class OrcamentoMapper {

    public OrcamentoResponse toResponse(Orcamento orcamento) {
        if (orcamento == null) return null;
        var dto = new OrcamentoResponse();
        dto.setId(orcamento.getId());
        dto.setOsId(orcamento.getOsId());
        dto.setStatus(orcamento.getStatus());
        dto.setItens(orcamento.getItens().stream().map(this::toItemResponse).toList());
        dto.setValorTotalPecas(orcamento.getValorTotalPecas());
        dto.setValorTotalServicos(orcamento.getValorTotalServicos());
        dto.setValorTotal(orcamento.getValorTotal());
        dto.setDataCriacao(orcamento.getDataCriacao());
        dto.setDataExpiracao(orcamento.getDataExpiracao());
        dto.setDataAtualizacao(orcamento.getDataAtualizacao());
        return dto;
    }

    private ItemOrcamentoResponse toItemResponse(ItemOrcamento item) {
        var dto = new ItemOrcamentoResponse();
        dto.setTipo(item.getTipo());
        dto.setIdTipo(item.getIdTipo());
        dto.setDescricao(item.getDescricao());
        dto.setQuantidade(item.getQuantidade());
        dto.setValorUnitario(item.getValorUnitario());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }
}

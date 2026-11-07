package com.fiap.techchallenge.pagamento.adapter.controller;

import com.fiap.techchallenge.pagamento.adapter.controller.dto.PagamentoResponse;
import com.fiap.techchallenge.pagamento.domain.Pagamento;
import org.springframework.stereotype.Component;

@Component
public class PagamentoMapper {

    public PagamentoResponse toResponse(Pagamento pagamento) {
        if (pagamento == null) return null;
        var dto = new PagamentoResponse();
        dto.setId(pagamento.getId());
        dto.setOsId(pagamento.getOsId());
        dto.setOrcamentoId(pagamento.getOrcamentoId());
        dto.setValor(pagamento.getValor());
        dto.setStatus(pagamento.getStatus());
        dto.setIdTransacaoProvedor(pagamento.getIdTransacaoProvedor());
        dto.setQrCode(pagamento.getQrCode());
        dto.setQrCodeBase64(pagamento.getQrCodeBase64());
        dto.setDataCriacao(pagamento.getDataCriacao());
        dto.setDataAtualizacao(pagamento.getDataAtualizacao());
        return dto;
    }
}

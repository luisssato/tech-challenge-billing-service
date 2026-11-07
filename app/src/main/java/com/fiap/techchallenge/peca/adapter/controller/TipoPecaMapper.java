package com.fiap.techchallenge.peca.adapter.controller;

import com.fiap.techchallenge.peca.adapter.controller.dto.TipoPecaResponse;
import com.fiap.techchallenge.peca.domain.TipoPeca;
import org.springframework.stereotype.Component;

@Component
public class TipoPecaMapper {

    public TipoPecaResponse toResponse(TipoPeca tipoPeca) {
        if (tipoPeca == null) return null;
        var dto = new TipoPecaResponse();
        dto.setId(tipoPeca.getId());
        dto.setNome(tipoPeca.getNome());
        dto.setValorUnitario(tipoPeca.getValorUnitario());
        return dto;
    }
}

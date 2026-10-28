package com.fiap.techchallenge.servico.adapter.controller;

import com.fiap.techchallenge.servico.adapter.controller.dto.TipoServicoResponse;
import com.fiap.techchallenge.servico.domain.TipoServico;
import org.springframework.stereotype.Component;

@Component
public class TipoServicoMapper {

    public TipoServicoResponse toResponse(TipoServico tipoServico) {
        if (tipoServico == null) return null;
        var dto = new TipoServicoResponse();
        dto.setId(tipoServico.getId());
        dto.setNome(tipoServico.getNome());
        dto.setValor(tipoServico.getValor());
        return dto;
    }
}

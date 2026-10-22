package com.fiap.techchallenge.orcamento.inputdata;

/**
 * Item vindo do diagnóstico concluído: referencia o tipo de peça ou de serviço do catálogo
 * e a quantidade (para serviços, normalmente 1).
 */
public record ItemDiagnosticoInputData(Long idTipo, int quantidade) {
}

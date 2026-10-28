package com.fiap.techchallenge.mensageria.domain;

public final class TipoEvento {

    // Eventos consumidos pelo billing-service
    public static final String DIAGNOSTICO_CONCLUIDO = "DIAGNOSTICO_CONCLUIDO";
    public static final String ORCAMENTO_APROVADO = "ORCAMENTO_APROVADO";
    public static final String EXECUCAO_FALHOU = "EXECUCAO_FALHOU";
    public static final String OS_CANCELADA = "OS_CANCELADA";

    // Eventos produzidos pelo billing-service
    public static final String ORCAMENTO_GERADO = "ORCAMENTO_GERADO";
    public static final String ORCAMENTO_REJEITADO = "ORCAMENTO_REJEITADO";
    public static final String PAGAMENTO_CONFIRMADO = "PAGAMENTO_CONFIRMADO";
    public static final String PAGAMENTO_RECUSADO = "PAGAMENTO_RECUSADO";
    public static final String PAGAMENTO_ESTORNADO = "PAGAMENTO_ESTORNADO";

    private TipoEvento() {}
}

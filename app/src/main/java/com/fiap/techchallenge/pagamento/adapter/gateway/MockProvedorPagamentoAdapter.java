package com.fiap.techchallenge.pagamento.adapter.gateway;

import com.fiap.techchallenge.pagamento.usecase.CobrancaQrCode;
import com.fiap.techchallenge.pagamento.usecase.ConsultaPagamento;
import com.fiap.techchallenge.pagamento.usecase.ProvedorPagamentoGateway;
import com.fiap.techchallenge.pagamento.usecase.StatusPagamentoProvedor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component("mockProvedorPagamentoAdapter")
@Slf4j
public class MockProvedorPagamentoAdapter implements ProvedorPagamentoGateway {

    private final Map<String, StatusPagamentoProvedor> statusSimulados = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> valoresSimulados = new ConcurrentHashMap<>();

    @Override
    public CobrancaQrCode criarCobranca(Long osId, String orcamentoId, BigDecimal valor, String descricao) {
        String idTransacao = "mock-mp-" + UUID.randomUUID();
        String qrCode = "00020101021243650016COM.MERCADOLIBRE0129https://mercadopago.com/pos/" + idTransacao;
        String qrCodeBase64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

        valoresSimulados.put(idTransacao, valor);

        log.info("[MOCK MP] Cobrança criada para OS {} com transação {}", osId, idTransacao);
        return new CobrancaQrCode(idTransacao, qrCode, qrCodeBase64);
    }

    @Override
    public ConsultaPagamento consultarPagamento(String idTransacao) {
        StatusPagamentoProvedor status = statusSimulados.get(idTransacao);
        if (status == null) {
            if (idTransacao != null && (idTransacao.toLowerCase().contains("recus") || idTransacao.toLowerCase().contains("reject"))) {
                status = StatusPagamentoProvedor.RECUSADO;
            } else if (idTransacao != null && (idTransacao.toLowerCase().contains("cancel"))) {
                status = StatusPagamentoProvedor.CANCELADO;
            } else {
                status = StatusPagamentoProvedor.APROVADO;
            }
            statusSimulados.put(idTransacao, status);
        }
        BigDecimal valor = valoresSimulados.getOrDefault(idTransacao, BigDecimal.ZERO);
        log.info("[MOCK MP] Consulta da transação {} -> {}", idTransacao, status);
        return new ConsultaPagamento(idTransacao, status, valor);
    }

    public void definirStatus(String idTransacao, StatusPagamentoProvedor status) {
        statusSimulados.put(idTransacao, status);
    }
}

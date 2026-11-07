package com.fiap.techchallenge.pagamento.adapter.gateway;

import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.pagamento.domain.Pagamento;
import com.fiap.techchallenge.pagamento.domain.StatusPagamento;
import com.fiap.techchallenge.pagamento.usecase.PagamentoGateway;
import com.fiap.techchallenge.sequence.SequenceGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class PagamentoGatewayImpl implements PagamentoGateway {

    private final DynamoDbClient dynamoDbClient;
    private final SequenceGenerator sequenceGenerator;
    private final String tableName;
    private final String paymentIndexName;

    public PagamentoGatewayImpl(DynamoDbClient dynamoDbClient,
                                SequenceGenerator sequenceGenerator,
                                @Value("${dynamodb.table.name:tech-challenge-prod-billing-service-table}") String tableName,
                                @Value("${dynamodb.gsi.payment:payment-id-index}") String paymentIndexName) {
        this.dynamoDbClient = dynamoDbClient;
        this.sequenceGenerator = sequenceGenerator;
        this.tableName = tableName;
        this.paymentIndexName = paymentIndexName;
    }

    @Override
    public Optional<Pagamento> findUltimoByOsId(Long osId) {
        if (osId == null) {
            return Optional.empty();
        }
        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("osId = :osId and begins_with(sk, :skPrefix)")
                .expressionAttributeValues(Map.of(
                        ":osId", AttributeValue.fromS(String.valueOf(osId)),
                        ":skPrefix", AttributeValue.fromS("PAGAMENTO#")
                ))
                .scanIndexForward(false)
                .limit(1)
                .build();

        QueryResponse response = dynamoDbClient.query(request);
        if (response.count() == 0 || response.items().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fromAttributes(response.items().get(0)));
    }

    @Override
    public Optional<Pagamento> findByIdTransacaoProvedor(String idTransacaoProvedor) {
        if (idTransacaoProvedor == null || idTransacaoProvedor.isBlank()) {
            return Optional.empty();
        }
        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .indexName(paymentIndexName)
                .keyConditionExpression("paymentId = :pid")
                .expressionAttributeValues(Map.of(
                        ":pid", AttributeValue.fromS(idTransacaoProvedor)
                ))
                .limit(1)
                .build();

        QueryResponse response = dynamoDbClient.query(request);
        if (response.count() == 0 || response.items().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fromAttributes(response.items().get(0)));
    }

    @Override
    public Pagamento save(Pagamento pagamento) {
        if (pagamento.getIdTransacaoProvedor() != null && !pagamento.getIdTransacaoProvedor().isBlank()) {
            Optional<Pagamento> existente = findByIdTransacaoProvedor(pagamento.getIdTransacaoProvedor());
            if (existente.isPresent() && !existente.get().getOsId().equals(pagamento.getOsId())) {
                throw new ConflictException("Já existe um pagamento com a transação " + pagamento.getIdTransacaoProvedor() + "!");
            }
        }

        Pagamento aSalvar = pagamento;
        if (pagamento.getId() == null) {
            long proximoId = sequenceGenerator.proximo("pagamento");
            aSalvar = new Pagamento(String.valueOf(proximoId), pagamento.getOsId(), pagamento.getOrcamentoId(),
                    pagamento.getValor(), pagamento.getStatus(), pagamento.getIdTransacaoProvedor(),
                    pagamento.getQrCode(), pagamento.getQrCodeBase64(), pagamento.getDataCriacao(), pagamento.getDataAtualizacao());
        }

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(toAttributes(aSalvar))
                .build();
        dynamoDbClient.putItem(request);
        return aSalvar;
    }

    private Map<String, AttributeValue> toAttributes(Pagamento domain) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("osId", AttributeValue.fromS(String.valueOf(domain.getOsId())));
        String idStr = domain.getId() != null ? domain.getId() : "0";
        long idNum;
        try {
            idNum = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            idNum = 0L;
        }
        item.put("sk", AttributeValue.fromS("PAGAMENTO#" + String.format("%010d", idNum)));
        item.put("id", AttributeValue.fromS(idStr));
        if (domain.getOrcamentoId() != null) {
            item.put("orcamentoId", AttributeValue.fromS(domain.getOrcamentoId()));
        }
        if (domain.getValor() != null) {
            item.put("valor", AttributeValue.fromS(domain.getValor().toPlainString()));
        }
        if (domain.getStatus() != null) {
            item.put("pagamentoStatus", AttributeValue.fromS(domain.getStatus().name()));
        }
        if (domain.getIdTransacaoProvedor() != null && !domain.getIdTransacaoProvedor().isBlank()) {
            item.put("paymentId", AttributeValue.fromS(domain.getIdTransacaoProvedor()));
        }
        if (domain.getQrCode() != null) {
            item.put("qrCode", AttributeValue.fromS(domain.getQrCode()));
        }
        if (domain.getQrCodeBase64() != null) {
            item.put("qrCodeBase64", AttributeValue.fromS(domain.getQrCodeBase64()));
        }
        if (domain.getDataCriacao() != null) {
            item.put("dataCriacao", AttributeValue.fromS(domain.getDataCriacao().toString()));
        }
        if (domain.getDataAtualizacao() != null) {
            item.put("dataAtualizacao", AttributeValue.fromS(domain.getDataAtualizacao().toString()));
        }
        return item;
    }

    private Pagamento fromAttributes(Map<String, AttributeValue> item) {
        String id = item.containsKey("id") ? item.get("id").s() : null;
        Long osId = Long.parseLong(item.get("osId").s());
        String orcamentoId = item.containsKey("orcamentoId") ? item.get("orcamentoId").s() : null;
        BigDecimal valor = item.containsKey("valor") ? new BigDecimal(item.get("valor").s()) : BigDecimal.ZERO;
        StatusPagamento status = item.containsKey("pagamentoStatus")
                ? StatusPagamento.valueOf(item.get("pagamentoStatus").s())
                : StatusPagamento.PENDENTE;
        String idTransacaoProvedor = item.containsKey("paymentId") ? item.get("paymentId").s() : null;
        String qrCode = item.containsKey("qrCode") ? item.get("qrCode").s() : null;
        String qrCodeBase64 = item.containsKey("qrCodeBase64") ? item.get("qrCodeBase64").s() : null;
        LocalDateTime dataCriacao = item.containsKey("dataCriacao") ? LocalDateTime.parse(item.get("dataCriacao").s()) : null;
        LocalDateTime dataAtualizacao = item.containsKey("dataAtualizacao") ? LocalDateTime.parse(item.get("dataAtualizacao").s()) : null;

        return new Pagamento(id, osId, orcamentoId, valor, status, idTransacaoProvedor, qrCode, qrCodeBase64, dataCriacao, dataAtualizacao);
    }
}

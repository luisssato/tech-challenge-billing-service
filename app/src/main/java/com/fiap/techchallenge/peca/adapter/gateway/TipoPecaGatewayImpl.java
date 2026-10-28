package com.fiap.techchallenge.peca.adapter.gateway;

import com.fiap.techchallenge.peca.domain.TipoPeca;
import com.fiap.techchallenge.peca.usecase.TipoPecaGateway;
import com.fiap.techchallenge.sequence.SequenceGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TipoPecaGatewayImpl implements TipoPecaGateway {

    static final String SEQUENCIA = "tipo_peca";
    static final String PK = "CATALOGO";
    static final String SK_PREFIX = "PECA#";

    private final DynamoDbClient dynamoDbClient;
    private final SequenceGenerator sequenceGenerator;
    private final String tableName;

    public TipoPecaGatewayImpl(DynamoDbClient dynamoDbClient,
                               SequenceGenerator sequenceGenerator,
                               @Value("${dynamodb.table.name:tech-challenge-prod-billing-service-table}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.sequenceGenerator = sequenceGenerator;
        this.tableName = tableName;
    }

    @Override
    public List<TipoPeca> findAll() {
        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("osId = :pk and begins_with(sk, :prefix)")
                .expressionAttributeValues(Map.of(
                        ":pk", AttributeValue.fromS(PK),
                        ":prefix", AttributeValue.fromS(SK_PREFIX)
                ))
                .build();

        QueryResponse response = dynamoDbClient.query(request);
        return response.items().stream()
                .map(this::fromAttributes)
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();
    }

    @Override
    public Optional<TipoPeca> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "osId", AttributeValue.fromS(PK),
                        "sk", AttributeValue.fromS(SK_PREFIX + id)
                ))
                .build();

        GetItemResponse response = dynamoDbClient.getItem(request);
        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fromAttributes(response.item()));
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    @Override
    public TipoPeca save(TipoPeca tipoPeca) {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(toAttributes(tipoPeca))
                .build();
        dynamoDbClient.putItem(request);
        return tipoPeca;
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) return;
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "osId", AttributeValue.fromS(PK),
                        "sk", AttributeValue.fromS(SK_PREFIX + id)
                ))
                .build();
        dynamoDbClient.deleteItem(request);
    }

    @Override
    public Long proximoId() {
        return sequenceGenerator.proximo(SEQUENCIA);
    }

    private Map<String, AttributeValue> toAttributes(TipoPeca domain) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("osId", AttributeValue.fromS(PK));
        item.put("sk", AttributeValue.fromS(SK_PREFIX + domain.getId()));
        item.put("id", AttributeValue.fromN(String.valueOf(domain.getId())));
        if (domain.getNome() != null) {
            item.put("nome", AttributeValue.fromS(domain.getNome()));
        }
        if (domain.getValorUnitario() != null) {
            item.put("valorUnitario", AttributeValue.fromS(domain.getValorUnitario().toPlainString()));
        }
        return item;
    }

    private TipoPeca fromAttributes(Map<String, AttributeValue> item) {
        Long id = Long.parseLong(item.get("id").n());
        String nome = item.containsKey("nome") ? item.get("nome").s() : "";
        BigDecimal valor = item.containsKey("valorUnitario")
                ? new BigDecimal(item.get("valorUnitario").s())
                : BigDecimal.ZERO;
        return new TipoPeca(id, nome, valor);
    }
}

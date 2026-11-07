package com.fiap.techchallenge.servico.adapter.gateway;

import com.fiap.techchallenge.sequence.SequenceGenerator;
import com.fiap.techchallenge.servico.domain.TipoServico;
import com.fiap.techchallenge.servico.usecase.TipoServicoGateway;
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
public class TipoServicoGatewayImpl implements TipoServicoGateway {

    static final String SEQUENCIA = "tipo_servico";
    static final String PK = "CATALOGO";
    static final String SK_PREFIX = "SERVICO#";

    private final DynamoDbClient dynamoDbClient;
    private final SequenceGenerator sequenceGenerator;
    private final String tableName;

    public TipoServicoGatewayImpl(DynamoDbClient dynamoDbClient,
                                  SequenceGenerator sequenceGenerator,
                                  @Value("${dynamodb.table.name:tech-challenge-prod-billing-service-table}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.sequenceGenerator = sequenceGenerator;
        this.tableName = tableName;
    }

    @Override
    public List<TipoServico> findAll() {
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
    public Optional<TipoServico> findById(Long id) {
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
    public TipoServico save(TipoServico tipoServico) {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(toAttributes(tipoServico))
                .build();
        dynamoDbClient.putItem(request);
        return tipoServico;
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

    private Map<String, AttributeValue> toAttributes(TipoServico domain) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("osId", AttributeValue.fromS(PK));
        item.put("sk", AttributeValue.fromS(SK_PREFIX + domain.getId()));
        item.put("id", AttributeValue.fromN(String.valueOf(domain.getId())));
        if (domain.getNome() != null) {
            item.put("nome", AttributeValue.fromS(domain.getNome()));
        }
        if (domain.getValor() != null) {
            item.put("valorUnitario", AttributeValue.fromS(domain.getValor().toPlainString()));
        }
        return item;
    }

    private TipoServico fromAttributes(Map<String, AttributeValue> item) {
        Long id = Long.parseLong(item.get("id").n());
        String nome = item.containsKey("nome") ? item.get("nome").s() : "";
        BigDecimal valor = item.containsKey("valorUnitario")
                ? new BigDecimal(item.get("valorUnitario").s())
                : BigDecimal.ZERO;
        return new TipoServico(id, nome, valor);
    }
}

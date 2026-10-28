package com.fiap.techchallenge.orcamento.adapter.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.orcamento.domain.ItemOrcamento;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.domain.StatusOrcamento;
import com.fiap.techchallenge.orcamento.domain.TipoItem;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoGateway;
import com.fiap.techchallenge.sequence.SequenceGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OrcamentoGatewayImpl implements OrcamentoGateway {

    private final DynamoDbClient dynamoDbClient;
    private final ObjectMapper objectMapper;
    private final SequenceGenerator sequenceGenerator;
    private final String tableName;

    public OrcamentoGatewayImpl(DynamoDbClient dynamoDbClient,
                                ObjectMapper objectMapper,
                                SequenceGenerator sequenceGenerator,
                                @Value("${dynamodb.table.name:tech-challenge-prod-billing-service-table}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.objectMapper = objectMapper;
        this.sequenceGenerator = sequenceGenerator;
        this.tableName = tableName;
    }

    @Override
    public Optional<Orcamento> findByOsId(Long osId) {
        if (osId == null) {
            return Optional.empty();
        }
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "osId", AttributeValue.fromS(String.valueOf(osId)),
                        "sk", AttributeValue.fromS("ORCAMENTO")
                ))
                .build();
        GetItemResponse response = dynamoDbClient.getItem(request);
        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fromAttributes(response.item()));
    }

    @Override
    public Orcamento save(Orcamento orcamento) {
        Orcamento aSalvar = orcamento;
        if (orcamento.getId() == null) {
            if (findByOsId(orcamento.getOsId()).isPresent()) {
                throw new ConflictException("Já existe um orçamento para a Ordem de Serviço " + orcamento.getOsId() + "!");
            }
            long proximoId = sequenceGenerator.proximo("orcamento");
            aSalvar = new Orcamento(String.valueOf(proximoId), orcamento.getOsId(), orcamento.getStatus(),
                    orcamento.getItens(), orcamento.getDataCriacao(), orcamento.getDataExpiracao(), orcamento.getDataAtualizacao());
        }

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(toAttributes(aSalvar))
                .build();
        dynamoDbClient.putItem(request);
        return aSalvar;
    }

    private Map<String, AttributeValue> toAttributes(Orcamento domain) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("osId", AttributeValue.fromS(String.valueOf(domain.getOsId())));
        item.put("sk", AttributeValue.fromS("ORCAMENTO"));
        if (domain.getId() != null) {
            item.put("id", AttributeValue.fromS(domain.getId()));
        }
        if (domain.getStatus() != null) {
            item.put("orcamentoStatus", AttributeValue.fromS(domain.getStatus().name()));
        }
        if (domain.getValorTotal() != null) {
            item.put("valorTotal", AttributeValue.fromS(domain.getValorTotal().toPlainString()));
        }
        try {
            List<ItemOrcamentoDto> dtos = domain.getItens() != null
                    ? domain.getItens().stream()
                    .map(i -> new ItemOrcamentoDto(i.getTipo().name(), i.getIdTipo(), i.getDescricao(), i.getQuantidade(), i.getValorUnitario()))
                    .toList()
                    : List.of();
            item.put("itensJson", AttributeValue.fromS(objectMapper.writeValueAsString(dtos)));
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao serializar itens do orçamento: " + e.getMessage(), e);
        }
        if (domain.getDataCriacao() != null) {
            item.put("dataCriacao", AttributeValue.fromS(domain.getDataCriacao().toString()));
        }
        if (domain.getDataExpiracao() != null) {
            item.put("dataExpiracao", AttributeValue.fromS(domain.getDataExpiracao().toString()));
        }
        if (domain.getDataAtualizacao() != null) {
            item.put("dataAtualizacao", AttributeValue.fromS(domain.getDataAtualizacao().toString()));
        }
        return item;
    }

    private Orcamento fromAttributes(Map<String, AttributeValue> item) {
        String id = item.containsKey("id") ? item.get("id").s() : null;
        Long osId = Long.parseLong(item.get("osId").s());
        StatusOrcamento status = item.containsKey("orcamentoStatus")
                ? StatusOrcamento.valueOf(item.get("orcamentoStatus").s())
                : StatusOrcamento.GERADO;

        List<ItemOrcamento> itens;
        try {
            String itensJson = item.containsKey("itensJson") ? item.get("itensJson").s() : "[]";
            List<ItemOrcamentoDto> dtos = objectMapper.readValue(itensJson, new TypeReference<List<ItemOrcamentoDto>>() {});
            itens = dtos.stream()
                    .map(d -> new ItemOrcamento(TipoItem.valueOf(d.getTipo()), d.getIdTipo(), d.getDescricao(), d.getQuantidade(), d.getValorUnitario()))
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao deserializar itens do orçamento: " + e.getMessage(), e);
        }

        LocalDateTime dataCriacao = item.containsKey("dataCriacao") ? LocalDateTime.parse(item.get("dataCriacao").s()) : null;
        LocalDateTime dataExpiracao = item.containsKey("dataExpiracao") ? LocalDateTime.parse(item.get("dataExpiracao").s()) : null;
        LocalDateTime dataAtualizacao = item.containsKey("dataAtualizacao") ? LocalDateTime.parse(item.get("dataAtualizacao").s()) : null;

        return new Orcamento(id, osId, status, itens, dataCriacao, dataExpiracao, dataAtualizacao);
    }

    public static class ItemOrcamentoDto {
        private String tipo;
        private Long idTipo;
        private String descricao;
        private int quantidade;
        private BigDecimal valorUnitario;

        public ItemOrcamentoDto() {}

        public ItemOrcamentoDto(String tipo, Long idTipo, String descricao, int quantidade, BigDecimal valorUnitario) {
            this.tipo = tipo;
            this.idTipo = idTipo;
            this.descricao = descricao;
            this.quantidade = quantidade;
            this.valorUnitario = valorUnitario;
        }

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public Long getIdTipo() { return idTipo; }
        public void setIdTipo(Long idTipo) { this.idTipo = idTipo; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public int getQuantidade() { return quantidade; }
        public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
        public BigDecimal getValorUnitario() { return valorUnitario; }
        public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    }
}

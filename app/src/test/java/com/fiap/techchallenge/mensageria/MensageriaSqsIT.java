package com.fiap.techchallenge.mensageria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.domain.StatusOrcamento;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoGateway;
import com.fiap.techchallenge.pagamento.domain.Pagamento;
import com.fiap.techchallenge.pagamento.domain.StatusPagamento;
import com.fiap.techchallenge.pagamento.usecase.PagamentoGateway;
import com.fiap.techchallenge.peca.domain.TipoPeca;
import com.fiap.techchallenge.peca.usecase.TipoPecaGateway;
import com.fiap.techchallenge.servico.domain.TipoServico;
import com.fiap.techchallenge.servico.usecase.TipoServicoGateway;
import com.fiap.techchallenge.support.DynamoDbTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "spring.cloud.aws.sqs.listener.auto-startup=true",
        "billing.outbox.enabled=true",
        "billing.outbox.intervalo-ms=200"
})
class MensageriaSqsIT extends DynamoDbTestSupport {

    @Autowired SqsAsyncClient sqs;
    @Autowired ObjectMapper mapper;
    @Autowired OrcamentoGateway orcamentoGateway;
    @Autowired PagamentoGateway pagamentoGateway;
    @Autowired TipoPecaGateway tipoPecaGateway;
    @Autowired TipoServicoGateway tipoServicoGateway;

    private final List<JsonNode> mensagensSaida = new CopyOnWriteArrayList<>();

    private String url(String nomeFila) {
        return sqs.getQueueUrl(r -> r.queueName(nomeFila)).join().queueUrl();
    }

    private void enviar(String nomeFila, String tipo, UUID eventId, String payload) {
        String corpo = "{\"eventId\":\"%s\",\"type\":\"%s\",\"version\":1,\"occurredAt\":\"2026-10-28T12:00:00Z\",\"correlationId\":\"x\",\"payload\":%s}"
                .formatted(eventId, tipo, payload);
        sqs.sendMessage(r -> r.queueUrl(url(nomeFila)).messageBody(corpo)).join();
    }

    private JsonNode aguardarMensagemSaida(long osId, String tipoEsperado) {
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            String url = url(FILA_SAIDA);
            for (Message m : sqs.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(10).waitTimeSeconds(1)).join().messages()) {
                mensagensSaida.add(mapper.readTree(m.body()));
                sqs.deleteMessage(r -> r.queueUrl(url).receiptHandle(m.receiptHandle())).join();
            }
            assertThat(mensagensSaida).anyMatch(e -> e.path("type").asText("").equals(tipoEsperado)
                    && e.path("correlationId").asText("").equals(String.valueOf(osId)));
        });

        return mensagensSaida.stream().filter(e -> e.path("type").asText("").equals(tipoEsperado)
                && e.path("correlationId").asText("").equals(String.valueOf(osId))).findFirst().orElseThrow();
    }

    @BeforeEach
    void setupCatalogo() {
        tipoPecaGateway.save(new TipoPeca(1L, "Óleo Motor", new BigDecimal("45.00")));
        tipoServicoGateway.save(new TipoServico(1L, "Troca de Óleo", new BigDecimal("50.00")));
    }

    @Test
    void diagnosticoConcluidoGeraOrcamentoEPublicaOrcamentoGeradoComIdempotencia() {
        long osId = 5001L;
        UUID eventId = UUID.randomUUID();

        String payload = """
                {
                  "osId": %d,
                  "itens": [
                    { "tipo": "PECA", "refId": 1, "quantidade": 2 },
                    { "tipo": "SERVICO", "refId": 1, "quantidade": 1 }
                  ]
                }
                """.formatted(osId);

        enviar(FILA_DIAGNOSTICO, "DIAGNOSTICO_CONCLUIDO", eventId, payload);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var opt = orcamentoGateway.findByOsId(osId);
            assertThat(opt).isPresent();
            assertThat(opt.get().getStatus()).isEqualTo(StatusOrcamento.GERADO);
            assertThat(opt.get().getValorTotal()).isEqualByComparingTo("140.00");
        });

        JsonNode eventoGerado = aguardarMensagemSaida(osId, "ORCAMENTO_GERADO");
        assertThat(eventoGerado.path("payload").path("osId").asLong()).isEqualTo(osId);
        assertThat(eventoGerado.path("payload").path("valorTotal").asDouble()).isEqualTo(140.00);

        // Teste de idempotência com o mesmo eventId: não duplica orçamento
        enviar(FILA_DIAGNOSTICO, "DIAGNOSTICO_CONCLUIDO", eventId, payload);
        var opt = orcamentoGateway.findByOsId(osId);
        assertThat(opt).isPresent();
    }

    @Test
    void execucaoFalhouExecutaCompensacaoEstornandoPagamentoAprovado() {
        long osId = 5002L;
        UUID eventId = UUID.randomUUID();

        // Cria pagamento já aprovado
        LocalDateTime agora = LocalDateTime.now();
        Pagamento aprovado = Pagamento.criar(osId, "orc-5002", new BigDecimal("140.00"), agora);
        aprovado.confirmar("mp-pago-5002", agora);
        pagamentoGateway.save(aprovado);

        String payload = """
                {
                  "osId": %d,
                  "motivo": "SEM_ESTOQUE",
                  "detalhe": "Peça 1 zerada no estoque"
                }
                """.formatted(osId);

        enviar(FILA_EXECUCAO_FALHOU, "EXECUCAO_FALHOU", eventId, payload);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var opt = pagamentoGateway.findUltimoByOsId(osId);
            assertThat(opt).isPresent();
            assertThat(opt.get().getStatus()).isEqualTo(StatusPagamento.ESTORNADO);
        });

        JsonNode eventoEstornado = aguardarMensagemSaida(osId, "PAGAMENTO_ESTORNADO");
        assertThat(eventoEstornado.path("payload").path("osId").asLong()).isEqualTo(osId);
        assertThat(eventoEstornado.path("payload").path("pagamentoId").asText()).isEqualTo("mp-pago-5002");
    }
}

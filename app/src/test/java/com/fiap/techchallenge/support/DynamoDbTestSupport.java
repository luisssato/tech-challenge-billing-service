package com.fiap.techchallenge.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

public abstract class DynamoDbTestSupport {

    public static final String TABLE_NAME = "tech-challenge-prod-billing-service-table";
    public static final String GSI_PAYMENT = "payment-id-index";

    public static final String FILA_DIAGNOSTICO = "billing-diagnostico-concluido";
    public static final String FILA_EXECUCAO_FALHOU = "billing-execucao-falhou";
    public static final String FILA_OS_CANCELADA = "billing-os-cancelada";
    public static final String FILA_SAIDA = "billing-events";

    protected static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
                    .withServices("sqs", "dynamodb");

    static {
        LOCALSTACK.start();
        try {
            LOCALSTACK.execInContainer("awslocal", "dynamodb", "create-table",
                    "--table-name", TABLE_NAME,
                    "--attribute-definitions",
                    "AttributeName=osId,AttributeType=S",
                    "AttributeName=sk,AttributeType=S",
                    "AttributeName=paymentId,AttributeType=S",
                    "--key-schema",
                    "AttributeName=osId,KeyType=HASH",
                    "AttributeName=sk,KeyType=RANGE",
                    "--global-secondary-indexes",
                    "[{\"IndexName\":\"" + GSI_PAYMENT + "\",\"KeySchema\":[{\"AttributeName\":\"paymentId\",\"KeyType\":\"HASH\"}],\"Projection\":{\"ProjectionType\":\"ALL\"}}]",
                    "--billing-mode", "PAY_PER_REQUEST");

            for (String fila : List.of(FILA_DIAGNOSTICO, FILA_EXECUCAO_FALHOU, FILA_OS_CANCELADA, FILA_SAIDA)) {
                LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", fila);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao inicializar recursos no LocalStack", e);
        }
    }

    @DynamicPropertySource
    static void configureAwsProperties(DynamicPropertyRegistry registry) {
        String endpoint = LOCALSTACK.getEndpoint().toString();
        registry.add("dynamodb.endpoint", () -> endpoint);
        registry.add("dynamodb.table.name", () -> TABLE_NAME);
        registry.add("dynamodb.gsi.payment", () -> GSI_PAYMENT);

        registry.add("spring.cloud.aws.region.static", () -> "us-east-1");
        registry.add("spring.cloud.aws.credentials.access-key", () -> "test");
        registry.add("spring.cloud.aws.credentials.secret-key", () -> "test");
        registry.add("spring.cloud.aws.sqs.endpoint", () -> endpoint);

        registry.add("billing.sqs.diagnostico-concluido", () -> FILA_DIAGNOSTICO);
        registry.add("billing.sqs.execucao-falhou", () -> FILA_EXECUCAO_FALHOU);
        registry.add("billing.sqs.os-cancelada", () -> FILA_OS_CANCELADA);
        registry.add("billing.sqs.saida", () -> FILA_SAIDA);
    }
}

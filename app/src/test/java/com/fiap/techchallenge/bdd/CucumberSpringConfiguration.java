package com.fiap.techchallenge.bdd;

import com.fiap.techchallenge.support.DynamoDbTestSupport;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest(properties = {
        "spring.cloud.aws.sqs.listener.auto-startup=true",
        "billing.outbox.enabled=true",
        "billing.outbox.intervalo-ms=200"
})
public class CucumberSpringConfiguration extends DynamoDbTestSupport {
}


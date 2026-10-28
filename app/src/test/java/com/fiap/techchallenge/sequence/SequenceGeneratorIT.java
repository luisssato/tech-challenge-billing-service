package com.fiap.techchallenge.sequence;

import com.fiap.techchallenge.config.DynamoDbConfig;
import com.fiap.techchallenge.support.DynamoDbTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {DynamoDbConfig.class, SequenceGenerator.class})
class SequenceGeneratorIT extends DynamoDbTestSupport {

    @Autowired
    SequenceGenerator generator;

    @Test
    void incrementaDeFormaSequencialEIndependentePorNome() {
        long primeiro = generator.proximo("it_a");
        long segundo = generator.proximo("it_a");
        long outraSequencia = generator.proximo("it_b");

        assertThat(primeiro).isGreaterThanOrEqualTo(1L);
        assertThat(segundo).isEqualTo(primeiro + 1);
        assertThat(outraSequencia).isGreaterThanOrEqualTo(1L);
    }
}

package com.fiap.techchallenge.peca.adapter.gateway;

import com.fiap.techchallenge.config.DynamoDbConfig;
import com.fiap.techchallenge.peca.domain.TipoPeca;
import com.fiap.techchallenge.sequence.SequenceGenerator;
import com.fiap.techchallenge.support.DynamoDbTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {DynamoDbConfig.class, SequenceGenerator.class, TipoPecaGatewayImpl.class})
class TipoPecaGatewayImplIT extends DynamoDbTestSupport {

    @Autowired
    TipoPecaGatewayImpl gateway;

    @Test
    void persisteEConsultaPorIdMantendoOValorMonetario() {
        gateway.save(new TipoPeca(701L, "Pastilha de freio", new BigDecimal("150.90")));

        TipoPeca encontrada = gateway.findById(701L).orElseThrow();
        assertThat(encontrada.getNome()).isEqualTo("Pastilha de freio");
        assertThat(encontrada.getValorUnitario()).isEqualByComparingTo("150.90");
        assertThat(gateway.existsById(701L)).isTrue();
        assertThat(gateway.findAll()).extracting(TipoPeca::getId).contains(701L);
    }

    @Test
    void removePorId() {
        gateway.save(new TipoPeca(702L, "Correia", new BigDecimal("80.00")));

        gateway.deleteById(702L);

        assertThat(gateway.findById(702L)).isEmpty();
        assertThat(gateway.existsById(702L)).isFalse();
    }

    @Test
    void geraIdsSequenciaisCrescentes() {
        Long primeiro = gateway.proximoId();
        Long segundo = gateway.proximoId();

        assertThat(segundo).isEqualTo(primeiro + 1);
    }
}

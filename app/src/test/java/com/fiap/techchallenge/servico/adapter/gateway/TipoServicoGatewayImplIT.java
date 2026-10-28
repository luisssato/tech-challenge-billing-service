package com.fiap.techchallenge.servico.adapter.gateway;

import com.fiap.techchallenge.config.DynamoDbConfig;
import com.fiap.techchallenge.sequence.SequenceGenerator;
import com.fiap.techchallenge.servico.domain.TipoServico;
import com.fiap.techchallenge.support.DynamoDbTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {DynamoDbConfig.class, SequenceGenerator.class, TipoServicoGatewayImpl.class})
class TipoServicoGatewayImplIT extends DynamoDbTestSupport {

    @Autowired
    TipoServicoGatewayImpl gateway;

    @Test
    void persisteEConsultaPorIdMantendoOValorMonetario() {
        gateway.save(new TipoServico(801L, "Alinhamento", new BigDecimal("150.90")));

        TipoServico encontrada = gateway.findById(801L).orElseThrow();
        assertThat(encontrada.getNome()).isEqualTo("Alinhamento");
        assertThat(encontrada.getValor()).isEqualByComparingTo("150.90");
        assertThat(gateway.existsById(801L)).isTrue();
        assertThat(gateway.findAll()).extracting(TipoServico::getId).contains(801L);
    }

    @Test
    void removePorId() {
        gateway.save(new TipoServico(802L, "Balanceamento", new BigDecimal("80.00")));

        gateway.deleteById(802L);

        assertThat(gateway.findById(802L)).isEmpty();
        assertThat(gateway.existsById(802L)).isFalse();
    }

    @Test
    void geraIdsSequenciaisCrescentes() {
        Long primeiro = gateway.proximoId();
        Long segundo = gateway.proximoId();

        assertThat(segundo).isEqualTo(primeiro + 1);
    }
}

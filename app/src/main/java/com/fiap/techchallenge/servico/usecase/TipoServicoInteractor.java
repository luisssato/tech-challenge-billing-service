package com.fiap.techchallenge.servico.usecase;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.exception.NotFoundException;
import com.fiap.techchallenge.servico.domain.TipoServico;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TipoServicoInteractor implements TipoServicoUseCase {

    private final TipoServicoGateway tipoServicoGateway;

    public TipoServicoInteractor(TipoServicoGateway tipoServicoGateway) {
        this.tipoServicoGateway = tipoServicoGateway;
    }

    @Override
    public List<TipoServico> getAllTiposServico() {
        return this.tipoServicoGateway.findAll();
    }

    @Override
    public Optional<TipoServico> getTipoServico(Long id) {
        return this.tipoServicoGateway.findById(id);
    }

    @Override
    public TipoServico createTipoServico(Long id, String nome, BigDecimal valor) {
        TipoServico tipoServico = TipoServico.criar(nome, valor);

        if (id != null) {
            if (id <= 0) {
                throw new BadRequestException("Id do tipo de serviço deve ser maior que zero!");
            }
            if (this.tipoServicoGateway.existsById(id)) {
                throw new ConflictException("Tipo de serviço de id " + id + " já existe!");
            }
            tipoServico.setId(id);
        } else {
            tipoServico.setId(gerarIdLivre());
        }

        return this.tipoServicoGateway.save(tipoServico);
    }

    @Override
    public TipoServico updateTipoServico(Long id, String nome, BigDecimal valor) {
        TipoServico tipoServico = buscarOuFalhar(id);
        tipoServico.atualizar(nome, valor);
        return this.tipoServicoGateway.save(tipoServico);
    }

    @Override
    public TipoServico deleteTipoServico(Long id) {
        TipoServico tipoServico = buscarOuFalhar(id);
        this.tipoServicoGateway.deleteById(id);
        return tipoServico;
    }

    private TipoServico buscarOuFalhar(Long id) {
        return this.tipoServicoGateway.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tipo de serviço de id {} não encontrado!", id);
                    return new NotFoundException("Tipo de serviço de id " + id + " não encontrado!");
                });
    }

    private Long gerarIdLivre() {
        Long id;
        do {
            id = this.tipoServicoGateway.proximoId();
        } while (this.tipoServicoGateway.existsById(id));
        return id;
    }
}

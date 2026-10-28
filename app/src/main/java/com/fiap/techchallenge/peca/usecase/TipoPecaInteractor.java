package com.fiap.techchallenge.peca.usecase;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.exception.NotFoundException;
import com.fiap.techchallenge.peca.domain.TipoPeca;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TipoPecaInteractor implements TipoPecaUseCase {

    private final TipoPecaGateway tipoPecaGateway;

    public TipoPecaInteractor(TipoPecaGateway tipoPecaGateway) {
        this.tipoPecaGateway = tipoPecaGateway;
    }

    @Override
    public List<TipoPeca> getAllTiposPeca() {
        return this.tipoPecaGateway.findAll();
    }

    @Override
    public Optional<TipoPeca> getTipoPeca(Long id) {
        return this.tipoPecaGateway.findById(id);
    }

    @Override
    public TipoPeca createTipoPeca(Long id, String nome, BigDecimal valorUnitario) {
        TipoPeca tipoPeca = TipoPeca.criar(nome, valorUnitario);

        if (id != null) {
            if (id <= 0) {
                throw new BadRequestException("Id do tipo de peça deve ser maior que zero!");
            }
            if (this.tipoPecaGateway.existsById(id)) {
                throw new ConflictException("Tipo de peça de id " + id + " já existe!");
            }
            tipoPeca.setId(id);
        } else {
            tipoPeca.setId(gerarIdLivre());
        }

        return this.tipoPecaGateway.save(tipoPeca);
    }

    @Override
    public TipoPeca updateTipoPeca(Long id, String nome, BigDecimal valorUnitario) {
        TipoPeca tipoPeca = buscarOuFalhar(id);
        tipoPeca.atualizar(nome, valorUnitario);
        return this.tipoPecaGateway.save(tipoPeca);
    }

    @Override
    public TipoPeca deleteTipoPeca(Long id) {
        TipoPeca tipoPeca = buscarOuFalhar(id);
        this.tipoPecaGateway.deleteById(id);
        return tipoPeca;
    }

    private TipoPeca buscarOuFalhar(Long id) {
        return this.tipoPecaGateway.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tipo de peça de id {} não encontrado!", id);
                    return new NotFoundException("Tipo de peça de id " + id + " não encontrado!");
                });
    }

    private Long gerarIdLivre() {
        Long id;
        do {
            id = this.tipoPecaGateway.proximoId();
        } while (this.tipoPecaGateway.existsById(id));
        return id;
    }
}

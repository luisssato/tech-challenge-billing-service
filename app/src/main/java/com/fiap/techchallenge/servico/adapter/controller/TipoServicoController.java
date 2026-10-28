package com.fiap.techchallenge.servico.adapter.controller;

import com.fiap.techchallenge.servico.adapter.controller.dto.CreateTipoServicoRequest;
import com.fiap.techchallenge.servico.adapter.controller.dto.TipoServicoResponse;
import com.fiap.techchallenge.servico.adapter.controller.dto.UpdateTipoServicoRequest;
import com.fiap.techchallenge.servico.usecase.TipoServicoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping({ "/api/tipo-servico", "/api/tipo-servico/" })
@Tag(name = "Tipos de Serviço", description = "Catálogo de preços dos serviços (mão de obra) oferecidos pela oficina")
public class TipoServicoController {

    private final TipoServicoUseCase tipoServicoUseCase;
    private final TipoServicoMapper mapper;

    public TipoServicoController(TipoServicoUseCase tipoServicoUseCase, TipoServicoMapper mapper) {
        this.tipoServicoUseCase = tipoServicoUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Listar tipos de serviço", description = "Retorna todos os tipos de serviço cadastrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    })
    public ResponseEntity<List<TipoServicoResponse>> getAllTiposServico() {
        var tiposServico = this.tipoServicoUseCase.getAllTiposServico().stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(tiposServico);
    }

    @GetMapping("{id}")
    @Operation(summary = "Buscar tipo de serviço por ID", description = "Retorna um tipo de serviço específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipo de serviço encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TipoServicoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tipo de serviço não encontrado", content = @Content)
    })
    public ResponseEntity<TipoServicoResponse> getTipoServicoById(@PathVariable Long id) {
        return this.tipoServicoUseCase.getTipoServico(id)
                .map(tipoServico -> ResponseEntity.ok(mapper.toResponse(tipoServico)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Criar tipo de serviço",
            description = "Cadastra o preço de um serviço. O id é o refId usado no evento DIAGNOSTICO_CONCLUIDO; sem id, é gerado sequencialmente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tipo de serviço criado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TipoServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para criação", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe um tipo de serviço com o id informado", content = @Content)
    })
    public ResponseEntity<TipoServicoResponse> createTipoServico(@Valid @RequestBody CreateTipoServicoRequest request) {
        var tipoServico = this.tipoServicoUseCase.createTipoServico(request.getId(), request.getNome(), request.getValor());
        return ResponseEntity.created(URI.create(tipoServico.getId().toString())).body(mapper.toResponse(tipoServico));
    }

    @PutMapping("{id}")
    @Operation(summary = "Atualizar tipo de serviço", description = "Atualiza nome e valor de um tipo de serviço")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipo de serviço atualizado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TipoServicoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tipo de serviço não encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para atualização", content = @Content)
    })
    public ResponseEntity<TipoServicoResponse> updateTipoServico(@PathVariable Long id, @RequestBody UpdateTipoServicoRequest request) {
        var tipoServico = this.tipoServicoUseCase.updateTipoServico(id, request.getNome(), request.getValor());
        return ResponseEntity.ok(mapper.toResponse(tipoServico));
    }

    @DeleteMapping("{id}")
    @Operation(summary = "Remover tipo de serviço", description = "Remove um tipo de serviço pelo identificador. Orçamentos já emitidos não são afetados (guardam o preço no momento da geração)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipo de serviço removido com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TipoServicoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tipo de serviço não encontrado", content = @Content)
    })
    public ResponseEntity<TipoServicoResponse> deleteTipoServico(@PathVariable Long id) {
        var tipoServico = this.tipoServicoUseCase.deleteTipoServico(id);
        return ResponseEntity.ok(mapper.toResponse(tipoServico));
    }
}

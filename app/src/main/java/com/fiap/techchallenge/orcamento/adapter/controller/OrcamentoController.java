package com.fiap.techchallenge.orcamento.adapter.controller;

import com.fiap.techchallenge.orcamento.adapter.controller.dto.OrcamentoResponse;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({ "/api/orcamento", "/api/orcamento/" })
@Tag(name = "Orçamentos", description = "Consulta e decisão do cliente sobre o orçamento de uma Ordem de Serviço. A geração é disparada pelo evento DIAGNOSTICO_CONCLUIDO")
public class OrcamentoController {

    private final OrcamentoUseCase orcamentoUseCase;
    private final OrcamentoMapper mapper;

    public OrcamentoController(OrcamentoUseCase orcamentoUseCase, OrcamentoMapper mapper) {
        this.orcamentoUseCase = orcamentoUseCase;
        this.mapper = mapper;
    }

    @GetMapping("{osId}")
    @Operation(summary = "Consultar orçamento da OS", description = "Retorna o orçamento vinculado à Ordem de Serviço")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orçamento encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrcamentoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado", content = @Content)
    })
    public ResponseEntity<OrcamentoResponse> getOrcamentoPorOs(@PathVariable Long osId) {
        return this.orcamentoUseCase.getOrcamentoPorOs(osId)
                .map(orcamento -> ResponseEntity.ok(mapper.toResponse(orcamento)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("{osId}/aprovar")
    @Operation(summary = "Aprovar orçamento", description = "O cliente aprova o orçamento. Só é possível no status GERADO e dentro da validade")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orçamento aprovado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrcamentoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Orçamento fora do status GERADO ou expirado", content = @Content)
    })
    public ResponseEntity<OrcamentoResponse> aprovarOrcamento(@PathVariable Long osId) {
        return ResponseEntity.ok(mapper.toResponse(this.orcamentoUseCase.aprovarOrcamento(osId)));
    }

    @PostMapping("{osId}/rejeitar")
    @Operation(summary = "Rejeitar orçamento", description = "O cliente rejeita o orçamento. Só é possível no status GERADO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orçamento rejeitado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrcamentoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Orçamento fora do status GERADO", content = @Content)
    })
    public ResponseEntity<OrcamentoResponse> rejeitarOrcamento(@PathVariable Long osId) {
        return ResponseEntity.ok(mapper.toResponse(this.orcamentoUseCase.rejeitarOrcamento(osId)));
    }
}

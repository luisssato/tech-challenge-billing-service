package com.fiap.techchallenge.pagamento.adapter.controller;

import com.fiap.techchallenge.pagamento.adapter.controller.dto.MercadoPagoWebhookNotification;
import com.fiap.techchallenge.pagamento.adapter.controller.dto.PagamentoResponse;
import com.fiap.techchallenge.pagamento.domain.Pagamento;
import com.fiap.techchallenge.pagamento.usecase.PagamentoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({ "/api/pagamento", "/api/pagamento/" })
@Tag(name = "Pagamentos", description = "Gestão de pagamentos via Mercado Pago / Pix e webhook de confirmação")
@Slf4j
public class PagamentoController {

    private final PagamentoUseCase pagamentoUseCase;
    private final PagamentoMapper mapper;

    public PagamentoController(PagamentoUseCase pagamentoUseCase, PagamentoMapper mapper) {
        this.pagamentoUseCase = pagamentoUseCase;
        this.mapper = mapper;
    }

    @GetMapping("{osId}")
    @Operation(summary = "Consultar pagamento da OS", description = "Retorna o pagamento mais recente da Ordem de Serviço")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagamento encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagamentoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado", content = @Content)
    })
    public ResponseEntity<PagamentoResponse> getPagamentoPorOs(@PathVariable Long osId) {
        return this.pagamentoUseCase.getPagamentoPorOs(osId)
                .map(pagamento -> ResponseEntity.ok(mapper.toResponse(pagamento)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("{osId}/iniciar")
    @Operation(summary = "Iniciar pagamento da OS",
            description = "Gera a cobrança Pix / QR Code dinâmico no Mercado Pago para um orçamento aprovado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cobrança Pix criada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagamentoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Orçamento não está APROVADO ou pagamento já foi aprovado", content = @Content)
    })
    public ResponseEntity<PagamentoResponse> iniciarPagamento(@PathVariable Long osId) {
        Pagamento pagamento = this.pagamentoUseCase.iniciarPagamento(osId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(pagamento));
    }

    @PostMapping("webhook")
    @Operation(summary = "Webhook do Mercado Pago",
            description = "Recebe notificações de alteração de status de pagamento do Mercado Pago (via query params ou JSON body)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificação processada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Identificador de transação não informado", content = @Content)
    })
    public ResponseEntity<Void> processarWebhook(
            @RequestParam(value = "data.id", required = false) String dataIdParam,
            @RequestParam(value = "id", required = false) String idParam,
            @RequestBody(required = false) MercadoPagoWebhookNotification body) {

        String idTransacao = null;
        if (dataIdParam != null && !dataIdParam.isBlank()) {
            idTransacao = dataIdParam;
        } else if (idParam != null && !idParam.isBlank()) {
            idTransacao = idParam;
        } else if (body != null) {
            idTransacao = body.extrairIdTransacao();
        }

        if (idTransacao == null || idTransacao.isBlank()) {
            log.warn("[WEBHOOK] Notificação recebida sem identificador de transação");
            return ResponseEntity.badRequest().build();
        }

        log.info("[WEBHOOK] Processando notificação para transação {}", idTransacao);
        this.pagamentoUseCase.processarNotificacaoWebhook(idTransacao);
        return ResponseEntity.ok().build();
    }
}


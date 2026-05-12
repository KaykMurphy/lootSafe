package com.lootSafe.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lootSafe.security.WebhookSignatureUtil;
import com.lootSafe.service.OfertaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    @Value("${lootsafe.secret_key}")
    private String webhookSecret;

    private final TaskExecutor taskExecutor;
    private final OfertaService ofertaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebhookController(@Qualifier("webhookTaskExecutor") TaskExecutor taskExecutor, OfertaService ofertaService) {
        this.taskExecutor = taskExecutor;
        this.ofertaService = ofertaService;
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> receberNotificacaoMP(
            @RequestBody String payloadCru,
            @RequestHeader(value = "x-signature", required = false) String assinatura,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestParam(value = "data.id", required = false) String dataId) {

        if (assinatura == null || requestId == null || dataId == null) {
            log.warn("Notificacao ignorada: Parametros de seguranca ausentes.");
            return ResponseEntity.status(400).build();
        }

        taskExecutor.execute(() -> {
            log.info("Processando webhook em background...");

            boolean isValido = WebhookSignatureUtil.isValidWebhook(assinatura, requestId, dataId, webhookSecret);

            if (isValido) {
                log.info("Assinatura verificada com sucesso.");

                try {
                    JsonNode jsonNode = objectMapper.readTree(payloadCru);
                    String tipo = jsonNode.has("type") ? jsonNode.get("type").asText() : "";

                    if ("payment".equals(tipo)) {
                        Long pagamentoId = Long.parseLong(dataId);
                        ofertaService.processarNotificacaoPagamento(pagamentoId);
                    } else {
                        log.info("Tipo de evento ignorado: {}", tipo);
                    }
                } catch (Exception e) {
                    log.error("Erro ao processar JSON: {}", e.getMessage());
                }
            } else {
                log.error("Assinatura invalida detectada.");
            }
        });

        return ResponseEntity.ok().build();
    }
}
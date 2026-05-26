package com.lootsafe.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lootsafe.security.WebhookSignatureUtil;
import com.lootsafe.service.OfferService;
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

    @Value("${lootsafe.secret-key}")
    private String webhookSecret;

    private final TaskExecutor taskExecutor;
    private final OfferService offerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebhookController(@Qualifier("webhookTaskExecutor") TaskExecutor taskExecutor, OfferService offerService) {
        this.taskExecutor = taskExecutor;
        this.offerService = offerService;
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> receiveMercadoPagoNotification(
            @RequestBody String rawPayload,
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestParam(value = "data.id", required = false) String dataId) {

        if (signature == null || requestId == null || dataId == null) {
            log.warn("Notificacao ignorada: parametros de seguranca ausentes.");
            return ResponseEntity.status(400).build();
        }

        taskExecutor.execute(() -> {
            log.info("Processando webhook em segundo plano...");

            boolean isValid = WebhookSignatureUtil.isValidWebhook(signature, requestId, dataId, webhookSecret);

            if (isValid) {
                log.info("Assinatura verificada com sucesso.");

                try {
                    JsonNode jsonNode = objectMapper.readTree(rawPayload);
                    String eventType = jsonNode.has("type") ? jsonNode.get("type").asText() : "";

                    if ("payment".equals(eventType)) {
                        Long paymentId = Long.parseLong(dataId);
                        offerService.processPaymentNotification(paymentId);
                    } else {
                        log.info("Tipo de evento ignorado: {}", eventType);
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

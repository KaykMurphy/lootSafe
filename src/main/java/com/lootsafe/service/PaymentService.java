package com.lootsafe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lootsafe.enums.PixKeyType;
import com.lootsafe.exception.PaymentProviderException;
import com.lootsafe.model.Offer;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentRefund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    private static final String TEST_PAYER_FIRST_NAME = "Test";
    private static final String TEST_PAYER_LAST_NAME = "User";
    private static final String TEST_PAYER_DOCUMENT_TYPE = "CPF";
    private static final String TEST_PAYER_DOCUMENT_NUMBER = "19119119100";

    @Value("${lootsafe.access-token}")
    private String accessToken;

    public Payment createPixPayment(
            Offer offer,
            String buyerEmail,
            String buyerFirstName,
            String buyerLastName,
            String documentType,
            String documentNumber
    ) throws MPException, MPApiException {
        MercadoPagoConfig.setAccessToken(accessToken);
        PaymentClient client = new PaymentClient();

        Map<String, String> headers = new HashMap<>();
        headers.put("x-idempotency-key", UUID.randomUUID().toString());

        String paymentDescription = StringUtils.hasText(offer.getDescription()) ? offer.getDescription() : "LootSafe offer payment";

        PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                .transactionAmount(offer.getGrossAmount())
                .description(paymentDescription)
                .paymentMethodId("pix")
                .externalReference(offer.getId().toString())
                .payer(createPayer(buyerEmail, buyerFirstName, buyerLastName, documentType, documentNumber))
                .build();

        return client.create(createRequest, MPRequestOptions.builder().customHeaders(headers).build());
    }

    private PaymentPayerRequest createPayer(
            String buyerEmail,
            String buyerFirstName,
            String buyerLastName,
            String documentType,
            String documentNumber
    ) {
        boolean isTestCredential = isTestCredential();

        String firstName = defaultIfBlank(buyerFirstName, isTestCredential ? TEST_PAYER_FIRST_NAME : "Buyer");
        String lastName = defaultIfBlank(buyerLastName, isTestCredential ? TEST_PAYER_LAST_NAME : "LootSafe");

        String normalizedDocumentType = defaultIfBlank(documentType, isTestCredential ? TEST_PAYER_DOCUMENT_TYPE : null);
        String normalizedDocumentNumber = digitsOnly(defaultIfBlank(documentNumber, isTestCredential ? TEST_PAYER_DOCUMENT_NUMBER : null));

        PaymentPayerRequest.PaymentPayerRequestBuilder builder = PaymentPayerRequest.builder()
                .email(buyerEmail)
                .firstName(firstName)
                .lastName(lastName);

        if (StringUtils.hasText(normalizedDocumentType) && StringUtils.hasText(normalizedDocumentNumber)) {
            builder.identification(
                    IdentificationRequest.builder()
                            .type(normalizedDocumentType.toUpperCase())
                            .number(normalizedDocumentNumber)
                            .build()
            );
        }

        return builder.build();
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String digitsOnly(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    public Payment getPayment(Long id) throws MPException, MPApiException {
        MercadoPagoConfig.setAccessToken(accessToken);
        PaymentClient client = new PaymentClient();
        return client.get(id);
    }

    public Payment cancelPix(Long id) throws MPException, MPApiException {
        MercadoPagoConfig.setAccessToken(accessToken);
        PaymentClient client = new PaymentClient();
        return client.cancel(id);
    }

    public PaymentRefund refundPayment(Long id) throws MPException, MPApiException {
        if (isTestCredential()) {
            log.info("SIMULACAO: Reembolso Mercado Pago ignorado para credencial TEST-. pagamentoId={}", id);
            return null;
        }

        MercadoPagoConfig.setAccessToken(accessToken);
        PaymentClient client = new PaymentClient();
        try {
            return client.refund(id);
        } catch (MPApiException e) {
            throw createProviderException("Mercado Pago rejected the refund", e);
        } catch (MPException e) {
            throw new PaymentProviderException(
                    "Mercado Pago communication error while refunding: " + e.getMessage(),
                    null,
                    null,
                    e
            );
        }
    }

    public void transferToSeller(String pixKey, PixKeyType pixKeyType, BigDecimal amount, UUID offerId) {
        if (isTestCredential()) {
            log.info("SIMULACAO: Repasse ao vendedor ignorado para credencial TEST-. ofertaId={} valor={}", offerId, amount);
            return;
        }

        MercadoPagoConfig.setAccessToken(accessToken);

        try {
            String mpKeyType = mapToMercadoPagoKeyType(pixKeyType);

            Map<String, Object> payload = new HashMap<>();
            payload.put("amount", amount);
            payload.put("external_reference", offerId.toString());

            Map<String, String> receiverAddress = new HashMap<>();
            receiverAddress.put("pix_key", pixKey);
            receiverAddress.put("pix_key_type", mpKeyType);
            payload.put("receiver_address", receiverAddress);

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mercadopago.com/v1/transfers"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .header("x-idempotency-key", UUID.randomUUID().toString())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Transferencia PIX enviada para o vendedor da oferta {}", offerId);
            } else {
                String detail = normalizeDetail(response.body());
                throw new PaymentProviderException(
                        "Mercado Pago rejected the seller transfer. HTTP " + response.statusCode() + ": " + detail,
                        response.statusCode(),
                        detail,
                        null
                );
            }

        } catch (PaymentProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Internal error while processing seller transfer: " + e.getMessage(), e);
        }
    }

    private String mapToMercadoPagoKeyType(PixKeyType type) {
        if (type == null) {
            return "RANDOM";
        }

        return switch (type) {
            case CPF -> "CPF";
            case CNPJ -> "CNPJ";
            case EMAIL -> "EMAIL";
            case PHONE -> "PHONE";
            default -> "RANDOM";
        };
    }

    private boolean isTestCredential() {
        return accessToken != null && accessToken.startsWith("TEST-");
    }

    private PaymentProviderException createProviderException(String operation, MPApiException e) {
        String detail = e.getApiResponse() == null ? e.getMessage() : normalizeDetail(e.getApiResponse().getContent());
        String message = operation + ". HTTP " + e.getStatusCode() + ": " + detail;
        return new PaymentProviderException(message, e.getStatusCode(), detail, e);
    }

    private String normalizeDetail(String detail) {
        if (!StringUtils.hasText(detail)) {
            return "no details in the response body";
        }

        String normalized = detail.replaceAll("\\s+", " ").trim();
        return normalized.length() > 600 ? normalized.substring(0, 600) + "..." : normalized;
    }
}

package com.lootsafe.service;

import com.lootsafe.enums.PixKeyType;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        boolean isTestCredential = accessToken != null && accessToken.startsWith("TEST-");

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
        MercadoPagoConfig.setAccessToken(accessToken);
        PaymentClient client = new PaymentClient();
        return client.refund(id);
    }

    public void transferToSeller(String pixKey, PixKeyType pixKeyType, BigDecimal amount, UUID offerId) {
        throw new UnsupportedOperationException(
                "Automatic PIX transfer has not been implemented yet for offer " + offerId
        );
    }
}

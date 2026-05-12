package com.lootSafe.service;

import com.lootSafe.dto.request.OfertaRequestDTO;
import com.mercadopago.MercadoPagoConfig;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PagamentoService {

    @Value("${lootsafe.access-token:token_nao_encontrado}")
    private String access_token;

    public Payment gerarPix(OfertaRequestDTO request) throws Exception {
        MercadoPagoConfig.setAccessToken(access_token);
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("x-idempotency-key", UUID.randomUUID().toString());

        MPRequestOptions requestOptions = MPRequestOptions.builder()
                .customHeaders(customHeaders)
                .build();

        PaymentClient client = new PaymentClient();

        PaymentCreateRequest paymentCreateRequest =
                PaymentCreateRequest.builder()
                        .transactionAmount(request.valorBruto())
                        .description(request.descricao())
                        .paymentMethodId("pix")
                        .dateOfExpiration(OffsetDateTime.now(ZoneOffset.UTC).plusHours(24))
                        .payer(PaymentPayerRequest.builder()
                                .email("comprador@lootsafe.com.br")
                                .build())
                        .build();

        return client.create(paymentCreateRequest, requestOptions);
    }

    public Payment consultarPagamento(Long idPagamento) throws Exception {
        MercadoPagoConfig.setAccessToken(access_token);
        PaymentClient client = new PaymentClient();
        return client.get(idPagamento);
    }

    public PaymentRefund reembolsoPagamento(Long idPagamento) throws MPException, MPApiException {
        MercadoPagoConfig.setAccessToken(access_token);

        try {
            PaymentClient client = new PaymentClient();
            PaymentRefund resultado = client.refund(idPagamento);

            log.info("Status do estorno para o pagamento {}: {}", idPagamento, resultado.getStatus());
            return resultado;

        } catch (MPException | MPApiException e) {
            log.error("Erro no Mercado Pago ao tentar estornar o pagamento {}: {}", idPagamento, e.getMessage());
            throw e;
        }
    }

    public PaymentRefund reembolsoParcial(Long idPagamento, BigDecimal valor) throws MPException, MPApiException{
        MercadoPagoConfig.setAccessToken(access_token);

        try{
            PaymentClient client = new PaymentClient();
            PaymentRefund devolucao = client.refund(idPagamento, valor);

            log.info("Status do estorno para o pagamento parcial {}: {}", idPagamento, devolucao.getStatus());
            return devolucao;
        }
        catch (MPException | MPApiException e) {
            log.error("Erro no Mercado Pago ao tentar estornar o pagamento {}: {}", idPagamento, e.getMessage());
            throw e;
        }
    }
}
package com.lootSafe.service;

import com.lootSafe.dto.request.OfertaRequestDTO;
import com.lootSafe.enums.TipoChavePix;
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

        PaymentCreateRequest paymentCreateRequest = PaymentCreateRequest.builder()
                .transactionAmount(request.valorBruto())
                .description(request.descricao())
                .paymentMethodId("pix")
                .dateOfExpiration(OffsetDateTime.now(ZoneOffset.UTC).plusHours(24))
                .payer(PaymentPayerRequest.builder()
                        .email(request.emailComprador())
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

    public PaymentRefund reembolsoParcial(Long idPagamento, BigDecimal valor) throws MPException, MPApiException {
        MercadoPagoConfig.setAccessToken(access_token);

        try {
            PaymentClient client = new PaymentClient();
            PaymentRefund devolucao = client.refund(idPagamento, valor);
            log.info("Status do estorno parcial para o pagamento {}: {}", idPagamento, devolucao.getStatus());
            return devolucao;

        } catch (MPException | MPApiException e) {
            log.error("Erro no Mercado Pago ao tentar estornar parcialmente o pagamento {}: {}",
                    idPagamento, e.getMessage());
            throw e;
        }
    }

    public void cancelarPix(Long idPagamento) {
        MercadoPagoConfig.setAccessToken(access_token);

        try {
            PaymentClient client = new PaymentClient();
            client.cancel(idPagamento);
            log.info("PIX {} cancelado no Mercado Pago com sucesso.", idPagamento);

        } catch (MPException | MPApiException e) {
            log.warn("Não foi possível cancelar o PIX {} no Mercado Pago: {}. " +
                            "Pode já ter expirado no gateway — cancelamento local prosseguirá.",
                    idPagamento, e.getMessage());
        }
    }

    public void enviarParaVendedor(String chavePix, TipoChavePix tipoChave, BigDecimal valorLiquido, UUID ofertaId)
            throws Exception {

        MercadoPagoConfig.setAccessToken(access_token);

        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("x-idempotency-key", "repasse-vendedor-" + ofertaId);

        MPRequestOptions requestOptions = MPRequestOptions.builder()
                .customHeaders(customHeaders)
                .build();

        PaymentClient client = new PaymentClient();

        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .transactionAmount(valorLiquido)
                .description("Repasse LootSafe - Oferta " + ofertaId)
                .paymentMethodId("pix")
                .payer(PaymentPayerRequest.builder()
                        .email("plataforma@lootsafe.com.br")
                        .build())
                .build();

        Payment resultado = client.create(request, requestOptions);

        log.info("Repasse para vendedor (oferta {}) iniciado. MP ID: {}, Status: {}, Chave PIX: {}",
                ofertaId, resultado.getId(), resultado.getStatus(), chavePix);
    }
}
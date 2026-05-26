package com.lootsafe.service;

import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.model.Offer;
import com.lootsafe.repository.OfferRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredPixScheduler {

    private final OfferRepository offerRepository;
    private final PaymentService paymentService;

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cancelExpiredPixPayments() {
        log.info("Iniciando varredura de PIX expirados (mais de 24h)...");

        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);

        List<Offer> unpaidPixPayments = offerRepository.findAllByTransactionStatusAndCreatedAtBefore(
                TransactionStatus.PENDING_PAYMENT,
                yesterday
        );

        for (Offer offer : unpaidPixPayments) {

            if (offer.getMercadoPagoPaymentId() != null) {
                try {
                    paymentService.cancelPix(offer.getMercadoPagoPaymentId());
                } catch (Exception e) {
                    log.warn("Falha ao cancelar PIX {} no Mercado Pago: {}",
                            offer.getMercadoPagoPaymentId(), e.getMessage());
                }
            }

            offer.setTransactionStatus(TransactionStatus.CANCELLED);
            offerRepository.save(offer);

            log.info("Oferta {} cancelada automaticamente porque o pagamento PIX expirou.", offer.getId());
        }
    }
}

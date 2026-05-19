package com.lootsafe.service;

import com.lootsafe.dto.response.OfferResponseDTO;
import com.lootsafe.enums.MediationDecision;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.mapper.OfferMapper;
import com.lootsafe.model.Offer;
import com.lootsafe.repository.OfferRepository;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediationService {

    private final OfferRepository offerRepository;
    private final PaymentService paymentService;
    private final OfferMapper offerMapper;

    public List<OfferResponseDTO> listOffersInMediation() {
        List<Offer> offers = offerRepository.findAllByTransactionStatus(TransactionStatus.IN_MEDIATION);
        return offerMapper.toResponseList(offers);
    }

    public OfferResponseDTO resolveDispute(UUID offerId, MediationDecision decision) throws MPException, MPApiException {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (offer.getTransactionStatus() != TransactionStatus.IN_MEDIATION) {
            throw new IllegalStateException("Only offers in mediation can be resolved.");
        }

        if (MediationDecision.BUYER_WINS.equals(decision)) {
            paymentService.refundPayment(offer.getMercadoPagoPaymentId());
            offer.setTransactionStatus(TransactionStatus.REFUNDED);
            log.info("Mediação da oferta {} resolvida a favor do comprador. Reembolso iniciado.", offerId);
        }

        if (MediationDecision.SELLER_WINS.equals(decision)) {

            try {
                paymentService.transferToSeller(
                        offer.getPixKey(),
                        offer.getPixKeyType(),
                        offer.getNetAmount(),
                        offer.getId()
                );
                offer.setTransactionStatus(TransactionStatus.SETTLED);
                log.info("Mediação da oferta {} resolvida a favor do vendedor. Repasse de {} iniciado.",
                        offerId, offer.getNetAmount());

            } catch (Exception e) {

                log.error("Falha ao enviar repasse para o vendedor da oferta {}: {}", offerId, e.getMessage());
                throw new RuntimeException(
                        "Decision was registered, but the seller transfer failed: " + e.getMessage(), e
                );
            }
        }

        Offer resolvedMediation = offerRepository.save(offer);
        return offerMapper.toResponseDTO(resolvedMediation);
    }

    public Page<OfferResponseDTO> listAllOffers(Pageable pageable) {
        return offerRepository.findAll(pageable)
                .map(offerMapper::toResponseDTO);
    }

    public OfferResponseDTO cancelManually(UUID offerId, boolean forceRefund) throws MPException, MPApiException {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (offer.getTransactionStatus() != TransactionStatus.PAYMENT_HELD
                && offer.getTransactionStatus() != TransactionStatus.IN_MEDIATION) {
            throw new IllegalStateException("The offer cannot be cancelled in its current status.");
        }

        if (forceRefund) {
            paymentService.refundPayment(offer.getMercadoPagoPaymentId());
        }

        offer.setTransactionStatus(TransactionStatus.CANCELLED);
        Offer savedOffer = offerRepository.save(offer);

        return offerMapper.toResponseDTO(savedOffer);
    }

    public BigDecimal calculatePlatformProfit() {

        BigDecimal profit = offerRepository.calculateTotalPlatformProfitByStatuses(
                List.of(TransactionStatus.SETTLED, TransactionStatus.COMPLETED)
        );

        return profit != null ? profit : BigDecimal.ZERO;
    }

    public OfferResponseDTO dropMediationByBuyer(UUID offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        TransactionStatus currentStatus = offer.getTransactionStatus();

        if (currentStatus != TransactionStatus.IN_MEDIATION) {
            throw new IllegalStateException("Apenas ofertas em mediação podem ser canceladas pelo comprador");
        }

        try {
            paymentService.transferToSeller(
                    offer.getPixKey(),
                    offer.getPixKeyType(),
                    offer.getNetAmount(),
                    offer.getId()
            );

            log.info("Comprador desistiu. Repasse de {} para vendedor iniciado.", offer.getNetAmount());
            offer.setTransactionStatus(TransactionStatus.SETTLED);
        } catch (Exception e) {
            log.error("Falha ao enviar repasse para o vendedor após o comprador desistir da mediação. offerId: {}", offerId, e);
            throw new RuntimeException("Falha no repasse automático. A oferta permanecerá em mediação para resolução manual.", e);
        }

        Offer offerSaved = offerRepository.save(offer);
        return offerMapper.toResponseDTO(offerSaved);
    }

    // TODO: Remove before production. Test-only endpoint.
    public OfferResponseDTO simulateApprovedPayment(UUID offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (offer.getTransactionStatus() != TransactionStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("The offer is not pending payment.");
        }

        offer.setTransactionStatus(TransactionStatus.PAYMENT_HELD);

        offer.setReleaseDeadline(java.time.LocalDateTime.now().plusHours(offer.getTrialPeriodHours()));

        log.info("SIMULAÇÃO: Pagamento da oferta {} aprovado manualmente para testes.", offerId);

        Offer savedOffer = offerRepository.save(offer);
        return offerMapper.toResponseDTO(savedOffer);
    }
}

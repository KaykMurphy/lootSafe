package com.lootsafe.service;

import com.lootsafe.dto.response.OfferResponseDTO;
import com.lootsafe.enums.MediationDecision;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.mapper.OfferMapper;
import com.lootsafe.model.EmailDetails;
import com.lootsafe.model.Offer;
import com.lootsafe.model.User;
import com.lootsafe.repository.EmailService;
import com.lootsafe.repository.OfferRepository;
import com.lootsafe.repository.UserRepository;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediationService {

    private final OfferRepository offerRepository;
    private final PaymentService paymentService;
    private final OfferMapper offerMapper;
    private final EmailService emailService;
    private final UserRepository userRepository;

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
            try {
                paymentService.refundPayment(offer.getMercadoPagoPaymentId());
                offer.setTransactionStatus(TransactionStatus.REFUNDED);
                log.info("Mediacao da oferta {} resolvida a favor do comprador. Reembolso iniciado.", offerId);
            } catch (MPException | MPApiException e) {
                log.error(
                        "Mediacao decidida a favor do comprador, mas o reembolso automatico via API falhou. ofertaId: {}",
                        offer.getId(),
                        e
                );
                throw new RuntimeException(
                        "Automatic refund failed. The offer will remain IN_MEDIATION until manual admin resolution.",
                        e
                );
            }

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
                log.info("Mediacao da oferta {} resolvida a favor do vendedor. Repasse de {} iniciado.",
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

    public OfferResponseDTO cancelManually(UUID offerId, boolean forceRefund) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        TransactionStatus currentStatus = offer.getTransactionStatus();
        if (currentStatus != TransactionStatus.PENDING_PAYMENT
                && currentStatus != TransactionStatus.PAYMENT_HELD
                && currentStatus != TransactionStatus.IN_MEDIATION) {
            throw new IllegalStateException("The offer cannot be cancelled/refunded in its current status.");
        }

        if (currentStatus == TransactionStatus.PENDING_PAYMENT) {
            if (offer.getMercadoPagoPaymentId() != null) {
                try {
                    paymentService.cancelPix(offer.getMercadoPagoPaymentId());
                } catch (MPException | MPApiException e) {
                    log.warn("Falha ao cancelar PIX no Mercado Pago (provavel expiracao natural). Ignorando e prosseguindo.");
                }
            }
            offer.setTransactionStatus(TransactionStatus.CANCELLED);

        } else if (currentStatus == TransactionStatus.PAYMENT_HELD || currentStatus == TransactionStatus.IN_MEDIATION) {
            if (!forceRefund) {
                throw new IllegalStateException("Payment has already been processed. forceRefund=true is required to refund it.");
            }

            try {
                paymentService.refundPayment(offer.getMercadoPagoPaymentId());
                offer.setTransactionStatus(TransactionStatus.REFUNDED);
            } catch (MPException | MPApiException e) {
                log.error("Falha critica ao reembolsar valor retido. ofertaId: {}", offerId, e);
                throw new RuntimeException("Could not process the automatic refund.", e);
            }
        }

        Offer savedOffer = offerRepository.save(offer);

        return offerMapper.toResponseDTO(savedOffer);
    }

    public BigDecimal calculatePlatformProfit() {
        BigDecimal profit = offerRepository.calculateTotalPlatformProfitByStatuses(
                List.of(TransactionStatus.SETTLED, TransactionStatus.COMPLETED)
        );
        return profit != null ? profit : BigDecimal.ZERO;
    }

    private EmailDetails getEmailDetails(Offer offer) {
        String body = String.format("""
                Hello! Your payment was approved.
                Product: %s
                Login: %s | Password: %s
                Room link: https://lootsafe.com.br/orders/%s
                """, offer.getProductCategory(), offer.getCredentialLogin(),
                offer.getCredentialPassword(), offer.getId());

        return new EmailDetails(offer.getBuyerEmail(), "LootSafe - Product Released!", body);
    }

    public OfferResponseDTO dropMediationByBuyer(UUID offerId, String loggedUserIdentifier) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        User user = userRepository.findByName(loggedUserIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equals(offer.getBuyerEmail())) {
            throw new AccessDeniedException("Você não tem permissão para cancelar a mediação desta oferta.");
        }

        return dropMediation(offer);
    }

    public OfferResponseDTO dropMediation(UUID offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        return dropMediation(offer);
    }

    private OfferResponseDTO dropMediation(Offer offer) {
        TransactionStatus currentStatus = offer.getTransactionStatus();

        if (currentStatus != TransactionStatus.IN_MEDIATION) {
            throw new IllegalStateException("Only offers in mediation can be dropped by the buyer.");
        }

        try {
            paymentService.transferToSeller(
                    offer.getPixKey(),
                    offer.getPixKeyType(),
                    offer.getNetAmount(),
                    offer.getId()
            );

            log.info("Comprador desistiu da mediacao. Repasse de {} para o vendedor iniciado.", offer.getNetAmount());
            offer.setTransactionStatus(TransactionStatus.SETTLED);
        } catch (Exception e) {
            log.error("Falha ao enviar repasse para o vendedor apos o comprador desistir da mediacao. ofertaId: {}", offer.getId(), e);
            throw new RuntimeException("Automatic transfer failed. The offer will remain in mediation for manual resolution.", e);
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
        offer.setReleaseDeadline(LocalDateTime.now().plusHours(offer.getTrialPeriodHours()));

        Offer savedOffer = offerRepository.save(offer);

        if (offer.getBuyerEmail() != null) {
            emailService.sendSimpleEmail(getEmailDetails(offer));
        } else {
            log.warn("SIMULACAO: Oferta {} nao possui email do comprador definido, e-mail nao enviado.", offerId);
        }

        log.info("SIMULACAO: Pagamento da oferta {} aprovado manualmente para testes.", offerId);
        return offerMapper.toResponseDTO(savedOffer);
    }
}

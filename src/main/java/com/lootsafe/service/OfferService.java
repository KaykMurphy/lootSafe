package com.lootsafe.service;

import com.lootsafe.dto.request.OfferUpdateDTO;
import com.lootsafe.dto.request.OfferRequestDTO;
import com.lootsafe.dto.response.OfferResponseDTO;
import com.lootsafe.dto.response.OfferSummaryResponseDTO;
import com.lootsafe.enums.Roles;
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
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentPointOfInteraction;
import com.mercadopago.resources.payment.PaymentTransactionData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final OfferMapper offerMapper;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${lootsafe.platform-fee-rate:0.10}")
    private BigDecimal platformFeeRate;

    @Transactional
    public OfferResponseDTO createOffer(OfferRequestDTO request) {
        Offer offer = offerMapper.toEntity(request);
        BigDecimal platformFee = request.grossAmount().multiply(platformFeeRate);
        offer.setPlatformFee(platformFee);
        offer.setNetAmount(request.grossAmount().subtract(platformFee));
        offer.setTransactionStatus(TransactionStatus.PENDING_PAYMENT);

        return offerMapper.toResponseDTO(offerRepository.save(offer));
    }

    @Transactional
    public OfferResponseDTO generatePixForBuyer(
            UUID id,
            String buyerEmail,
            String buyerFirstName,
            String buyerLastName,
            String documentType,
            String documentNumber
    ) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (offer.getTransactionStatus() != TransactionStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("PIX can only be generated for offers pending payment.");
        }

        try {
            Payment payment = paymentService.createPixPayment(
                    offer,
                    buyerEmail,
                    buyerFirstName,
                    buyerLastName,
                    documentType,
                    documentNumber
            );
            PaymentTransactionData pixData = extractPixData(payment);
            offer.setBuyerEmail(buyerEmail);
            offer.setMercadoPagoPaymentId(payment.getId());
            offer.setPixCopyPaste(pixData.getQrCode());
            offer.setPixQrCode(pixData.getQrCodeBase64());
        } catch (MPApiException e) {
            String detail = getMercadoPagoErrorDetail(e);
            log.error("Erro ao gerar PIX no Mercado Pago. status={} corpo={}", e.getStatusCode(), detail, e);
            throw new IllegalStateException("Mercado Pago rejected PIX generation: " + detail, e);
        } catch (MPException e) {
            log.error("Erro de comunicacao ao gerar PIX no Mercado Pago: {}", e.getMessage(), e);
            throw new IllegalStateException("Mercado Pago communication error: " + e.getMessage(), e);
        }

        return offerMapper.toResponseDTO(offerRepository.save(offer));
    }

    private PaymentTransactionData extractPixData(Payment payment) {
        PaymentPointOfInteraction pointOfInteraction = payment.getPointOfInteraction();

        if (pointOfInteraction == null || pointOfInteraction.getTransactionData() == null) {
            throw new IllegalStateException("Mercado Pago did not return PIX QR Code data.");
        }

        PaymentTransactionData transactionData = pointOfInteraction.getTransactionData();

        if (transactionData.getQrCode() == null || transactionData.getQrCodeBase64() == null) {
            throw new IllegalStateException("Mercado Pago returned PIX without copy-paste code or QR Code.");
        }

        return transactionData;
    }

    private String getMercadoPagoErrorDetail(MPApiException e) {
        if (e.getApiResponse() == null || e.getApiResponse().getContent() == null || e.getApiResponse().getContent().isBlank()) {
            return e.getMessage();
        }

        String content = e.getApiResponse().getContent().replaceAll("\\s+", " ").trim();
        return content.length() > 600 ? content.substring(0, 600) + "..." : content;
    }

    @Transactional
    public void processPaymentNotification(Long mercadoPagoPaymentId) {
        try {
            Payment payment = paymentService.getPayment(mercadoPagoPaymentId);

            if ("approved".equals(payment.getStatus())) {
                Offer offer = offerRepository.findByMercadoPagoPaymentId(mercadoPagoPaymentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

                if (offer.getTransactionStatus() == TransactionStatus.PENDING_PAYMENT) {
                    releaseProductAndScheduleEmail(offer);
                }
            }
        } catch (Exception e) {
            log.error("Erro no webhook ao processar pagamento {}: {}", mercadoPagoPaymentId, e.getMessage(), e);
        }
    }

    private void releaseProductAndScheduleEmail(Offer offer) {
        offer.setReleaseDeadline(LocalDateTime.now().plusHours(offer.getTrialPeriodHours()));
        offer.setTransactionStatus(TransactionStatus.PAYMENT_HELD);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Commit realizado. Enviando e-mail para {}", offer.getBuyerEmail());
                emailService.sendSimpleEmail(getEmailDetails(offer));
            }
        });

        offerRepository.save(offer);
    }

    public OfferResponseDTO updateOffer(UUID id, OfferUpdateDTO dto, String loggedUserIdentifier) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        User user = userRepository.findByName(loggedUserIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isOfferOwner = user.getEmail().equals(offer.getSellerEmail());
        boolean isModerator = user.getRoles().contains(Roles.MODERADOR);

        if (!isModerator && !isOfferOwner) {
            throw new AccessDeniedException("Você não tem permissão para modificar esta oferta.");
        }

        offerMapper.updateEntityFromDto(dto, offer);

        if (dto.grossAmount() != null) {
            BigDecimal platformFee = dto.grossAmount().multiply(platformFeeRate);
            offer.setPlatformFee(platformFee);
            offer.setNetAmount(dto.grossAmount().subtract(platformFee));
        }

        return offerMapper.toResponseDTO(offerRepository.save(offer));
    }

    @Transactional
    public OfferResponseDTO releasePayment(UUID id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (offer.getTransactionStatus() != TransactionStatus.PAYMENT_HELD) {
            throw new IllegalStateException("Payment can only be released while it is held.");
        }

        paymentService.transferToSeller(
                offer.getPixKey(),
                offer.getPixKeyType(),
                offer.getNetAmount(),
                offer.getId()
        );
        offer.setTransactionStatus(TransactionStatus.SETTLED);

        return offerMapper.toResponseDTO(offerRepository.save(offer));
    }

    public Page<OfferSummaryResponseDTO> listAll(Pageable pageable) {
        return offerRepository.findAll(pageable).map(offerMapper::toSummaryResponseDTO);
    }

    @Transactional
    public void deleteOffer(UUID id, String loggedUserIdentifier) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        User user = userRepository.findByName(loggedUserIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isModerator = user.getRoles().contains(Roles.MODERADOR);
        boolean isOfferOwner = user.getEmail().equals(offer.getSellerEmail());

        if (!isModerator && !isOfferOwner) {
            throw new AccessDeniedException("Você não tem permissão para deletar esta oferta.");
        }

        List<TransactionStatus> blockedStatuses = List.of(
                TransactionStatus.PAYMENT_HELD,
                TransactionStatus.COMPLETED,
                TransactionStatus.IN_MEDIATION
        );

        if (blockedStatuses.contains(offer.getTransactionStatus())) {
            throw new IllegalStateException(
                    "Cannot delete an offer with status: " + offer.getTransactionStatus()
                            + ". Cancel it before removing it."
            );
        }

        offerRepository.delete(offer);
    }

    @Transactional
    public OfferResponseDTO openMediation(UUID id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (offer.getTransactionStatus() != TransactionStatus.PAYMENT_HELD) {
            throw new IllegalStateException("Mediation can only be opened for held payments.");
        }

        offer.setTransactionStatus(TransactionStatus.IN_MEDIATION);
        return offerMapper.toResponseDTO(offerRepository.save(offer));
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

    public OfferResponseDTO getById(UUID id) {
        return offerRepository.findById(id)
                .map(offerMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));
    }
}

package com.lootsafe.dto.response;

import com.lootsafe.enums.ProductCategory;
import com.lootsafe.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OfferSummaryResponseDTO(
        UUID id,
        ProductCategory productCategory,
        String description,
        BigDecimal grossAmount,
        TransactionStatus transactionStatus,
        LocalDateTime createdAt,
        LocalDateTime linkExpiresAt,
        Integer trialPeriodHours
) {}

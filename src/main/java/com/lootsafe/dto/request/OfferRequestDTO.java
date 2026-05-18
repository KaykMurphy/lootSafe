package com.lootsafe.dto.request;

import com.lootsafe.enums.ProductCategory;
import com.lootsafe.enums.PixKeyType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OfferRequestDTO(

        @NotNull
        ProductCategory productCategory,

        @NotBlank
        String description,

        @NotNull
        @Positive
        BigDecimal grossAmount,

        @NotNull
        @Positive
        Integer trialPeriodHours,

        @NotBlank
        String credentialLogin,

        @NotBlank
        String credentialPassword,

        @NotBlank
        @Email
        String sellerEmail,

        @NotNull
        PixKeyType pixKeyType,

        @NotBlank
        String pixKey

) {}

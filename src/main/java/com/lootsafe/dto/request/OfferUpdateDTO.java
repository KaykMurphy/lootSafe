package com.lootsafe.dto.request;

import com.lootsafe.enums.ProductCategory;
import com.lootsafe.enums.PixKeyType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OfferUpdateDTO(

        ProductCategory productCategory,
        String description,

        @Positive
        BigDecimal grossAmount,

        @Positive
        Integer trialPeriodHours,

        String credentialLogin,
        String credentialPassword,

        @Email
        String sellerEmail,
        @Email
        String buyerEmail,

        PixKeyType pixKeyType,
        String pixKey

) {
}

package com.lootsafe.model;

import com.lootsafe.enums.ProductCategory;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.enums.PixKeyType;
import com.lootsafe.security.CryptoConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductCategory productCategory;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal grossAmount;

    @Column(columnDefinition = "TEXT")
    private String pixQrCode;

    @Column(columnDefinition = "TEXT")
    private String pixCopyPaste;

    private Long mercadoPagoPaymentId;

    @Column(nullable = false)
    private BigDecimal platformFee;

    @Column(nullable = false)
    private BigDecimal netAmount;

    @Column(nullable = false)
    private Integer trialPeriodHours;

    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;

    @Column(nullable = false)
    @Convert(converter = CryptoConverter.class)
    private String credentialLogin;

    @Column(nullable = false)
    @Convert(converter = CryptoConverter.class)
    private String credentialPassword;

    @Column(nullable = false)
    private String sellerEmail;

    private String buyerEmail;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PixKeyType pixKeyType;

    @Column(nullable = false)
    private String pixKey;

    private LocalDateTime createdAt;

    private LocalDateTime linkExpiresAt;

    private LocalDateTime releaseDeadline;

    @PrePersist
    private void beforePersist() {
        this.createdAt = LocalDateTime.now();
        this.linkExpiresAt = LocalDateTime.now().plusHours(48);
    }
}

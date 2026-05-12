package com.lootSafe.model;

import com.lootSafe.enums.CategoriaProduto;
import com.lootSafe.enums.StatusTransacao;
import com.lootSafe.enums.TipoChavePix;
import com.lootSafe.security.CryptoConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CategoriaProduto categoriaProduto;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private BigDecimal valorBruto;

    @Column(columnDefinition = "TEXT")
    private String qrCodePix;

    @Column(columnDefinition = "TEXT")
    private String copiaEColaPix;
    private Long mercadoPagoId;

    @Column(nullable = false)
    private BigDecimal taxaPlataforma;

    @Column(nullable = false)
    private BigDecimal valorLiquido;

    @Column(nullable = false)
    private Integer prazoTesteHoras;

    @Enumerated(EnumType.STRING)
    private StatusTransacao statusTransacao;

    @Column(nullable = false)
    @Convert(converter = CryptoConverter.class)
    private String loginCredencial;

    @Column(nullable = false)
    @Convert(converter = CryptoConverter.class)
    private String senhaCredencial;

    @Column(nullable = false)
    private String emailVendedor;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoChavePix tipoChavePix;

    @Column(nullable = false)
    private String chavePix;

    private LocalDateTime dataCriacao;

    private LocalDateTime dataExpiracaoLink;

    private LocalDateTime dataLimiteLiberacao;

    @PrePersist
    private void beforePersist() {
        this.dataCriacao = LocalDateTime.now();
        this.dataExpiracaoLink = LocalDateTime.now().plusHours(48);
    }





}

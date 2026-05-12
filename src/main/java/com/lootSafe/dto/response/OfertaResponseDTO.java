package com.lootSafe.dto.response;

import com.lootSafe.enums.CategoriaProduto;
import com.lootSafe.enums.StatusTransacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OfertaResponseDTO(

        UUID id,
        CategoriaProduto categoriaProduto,
        String descricao,
        BigDecimal valorBruto,
        BigDecimal taxaPlataforma,
        BigDecimal valorLiquido,
        StatusTransacao statusTransacao,
        LocalDateTime dataCriacao,
        LocalDateTime dataExpiracaoLink,

        String qrCodePix,
        String copiaEColaPix,
        Long mercadoPagoId


) {
}

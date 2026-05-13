package com.lootSafe.dto.request;

import com.lootSafe.enums.CategoriaProduto;
import com.lootSafe.enums.TipoChavePix;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OfertaRequestDTO(

        @NotNull
        CategoriaProduto categoriaProduto,

        @NotBlank
        String descricao,

        @NotNull
        @Positive
        BigDecimal valorBruto,

        @NotNull
        @Positive
        Integer prazoTesteHoras,

        @NotBlank
        String loginCredencial,

        @NotBlank
        String senhaCredencial,

        @NotBlank
        @Email
        String emailVendedor,

        @NotBlank
        @Email
        String emailComprador,

        @NotNull
        TipoChavePix tipoChavePix,

        @NotBlank
        String chavePix

) {}
package com.lootSafe.dto.request;

import com.lootSafe.enums.CategoriaProduto;
import com.lootSafe.enums.TipoChavePix;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OfertaAtualizarDTO(

        CategoriaProduto categoriaProduto,
        String descricao,

        @Positive
        BigDecimal valorBruto,

        @Positive
        Integer prazoTesteHoras,

        String loginCredencial,
        String senhaCredencial,

        @Email
        String emailVendedor,

        TipoChavePix tipoChavePix,
        String chavePix

) {
}

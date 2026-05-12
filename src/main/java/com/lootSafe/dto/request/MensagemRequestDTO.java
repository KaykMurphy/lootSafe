package com.lootSafe.dto.request;

import com.lootSafe.enums.AutorMensagem;

public record MensagemRequestDTO(

        AutorMensagem autorMensagem,
        String textoMensagem
) {

}

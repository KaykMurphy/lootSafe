package com.lootSafe.dto.response;

import com.lootSafe.enums.AutorMensagem;

import java.time.LocalDateTime;
import java.util.UUID;

public record MensagemResponseDTO (

        UUID id,
        AutorMensagem autor,
        String textoMensagem,
        LocalDateTime dataEnvio
)
{ }

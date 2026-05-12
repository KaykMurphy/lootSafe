package com.lootSafe.model;


public record EmailDetails(
        String destinatario,
        String assunto,
        String corpoMensagem
) {
}

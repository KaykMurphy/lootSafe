package com.lootSafe.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CryptoProperties {

    public static String chaveCriptografia;

    @Value("${lootsafe.seguranca.chave-criptografia}")
    private String chaveInjetada;

    private static final Set<Integer> TAMANHOS_VALIDOS_AES = Set.of(16, 24, 32);

    @PostConstruct
    public void init() {
        if (chaveInjetada == null || chaveInjetada.isBlank()) {
            throw new IllegalStateException(
                    "[LootSafe] A chave de criptografia (lootsafe.seguranca.chave-criptografia) " +
                            "não foi configurada. A aplicação não pode iniciar sem ela."
            );
        }

        int tamanho = chaveInjetada.getBytes().length;
        if (!TAMANHOS_VALIDOS_AES.contains(tamanho)) {
            throw new IllegalStateException(
                    "[LootSafe] Tamanho de chave AES inválido: " + tamanho + " bytes. " +
                            "A chave deve ter exatamente 16, 24 ou 32 bytes (128, 192 ou 256 bits)."
            );
        }

        CryptoProperties.chaveCriptografia = chaveInjetada;
    }
}
package com.lootsafe.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Set;

@Component
public class CryptoProperties {

    public static String encryptionKey;

    @Value("${lootsafe.security.encryption-key}")
    private String configuredKey;

    private static final Set<Integer> VALID_AES_KEY_SIZES = Set.of(16, 24, 32);

    @PostConstruct
    public void init() {
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException(
                    "[LootSafe] The encryption key (lootsafe.security.encryption-key) " +
                            "was not configured. The application cannot start without it."
            );
        }

        byte[] decodedKey = Base64.getDecoder().decode(configuredKey);
        if (!VALID_AES_KEY_SIZES.contains(decodedKey.length)) {
            throw new IllegalStateException(
                    "[LootSafe] Invalid AES key size: " + decodedKey.length + " bytes. " +
                            "The key must be exactly 16, 24, or 32 bytes (128, 192, or 256 bits)."
            );
        }

        CryptoProperties.encryptionKey = configuredKey;
    }
}

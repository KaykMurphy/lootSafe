package com.lootSafe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CryptoProperties {

    public static String chaveCriptografia = "LootSafeChave123";

    @Value("${lootsafe.seguranca.chave-criptografia}")
    public void setChaveCriptografia(String chave) {
        CryptoProperties.chaveCriptografia = chave;
    }
}
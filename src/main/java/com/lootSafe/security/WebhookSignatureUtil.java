package com.lootSafe.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;

import java.security.MessageDigest;

@Slf4j
public class WebhookSignatureUtil {

    public static boolean isValidWebhook(String xSignature, String requestId, String dataId, String secret) {
        try {
            String[] parts = xSignature.split(",");
            String ts = "";
            String v1 = "";

            for (String part : parts) {
                if (part.trim().startsWith("ts=")) ts = part.trim().substring(3);
                else if (part.trim().startsWith("v1=")) v1 = part.trim().substring(3);
            }

            if (ts.isEmpty() || v1.isEmpty()) return false;

            String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";

            String hashGerado = new HmacUtils("HmacSHA256", secret).hmacHex(manifest);

            return MessageDigest.isEqual(hashGerado.getBytes(), v1.getBytes());

        } catch (Exception e) {
            log.error("Falha critica na validacao da assinatura: {}", e.getMessage());
            return false;
        }
    }
}
package com.guido.scenicai.common.security;

import com.guido.scenicai.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CredentialCrypto {

    private static final String PREFIX = "v1";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String rawKey;

    public CredentialCrypto(@Value("${ai.config.aes-key:}") String rawKey) {
        this.rawKey = rawKey;
    }

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return PREFIX + ":" + Base64.getEncoder().encodeToString(iv)
                    + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new BizException(500, "凭证加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText) || !cipherText.startsWith(PREFIX + ":")) {
            return cipherText;
        }
        try {
            String[] parts = cipherText.split(":", 3);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BizException(500, "凭证解密失败", e);
        }
    }

    private SecretKeySpec buildKey() throws Exception {
        if (!StringUtils.hasText(rawKey) || rawKey.length() < 16) {
            throw new BizException(500, "AI_CONFIG_AES_KEY 未配置或长度不足");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }
}

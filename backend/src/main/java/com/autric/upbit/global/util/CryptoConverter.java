package com.autric.upbit.global.util;

import com.autric.upbit.global.config.properties.AppCryptoProperties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256 암호화/복호화 JPA Converter
 * DB 저장 시 자동 암호화, 조회 시 자동 복호화
 */
@Slf4j
@Converter
@Component
public class CryptoConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int IV_SIZE = 16;

    private final AppCryptoProperties cryptoProperties;

    public CryptoConverter(AppCryptoProperties cryptoProperties) {
        this.cryptoProperties = cryptoProperties;
    }

    /**
     * DB 저장 시: 평문 → 암호화
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }

        try {
            return encrypt(attribute);
        } catch (Exception e) {
            log.error("암호화 실패", e);
            throw new RuntimeException("데이터 암호화에 실패했습니다.", e);
        }
    }

    /**
     * DB 조회 시: 암호화 → 평문
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        try {
            return decrypt(dbData);
        } catch (Exception e) {
            log.error("복호화 실패", e);
            throw new RuntimeException("데이터 복호화에 실패했습니다.", e);
        }
    }

    /**
     * AES-256 암호화
     * IV(Initialization Vector)를 랜덤 생성하여 암호문 앞에 붙임
     */
    private String encrypt(String plainText) throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[IV_SIZE];
        random.nextBytes(iv);

        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        SecretKeySpec keySpec = new SecretKeySpec(getKeyBytes(), KEY_ALGORITHM);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[IV_SIZE + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, IV_SIZE);
        System.arraycopy(encrypted, 0, combined, IV_SIZE, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * AES-256 복호화
     * 암호문 앞 16바이트를 IV로 추출
     */
    private String decrypt(String encryptedText) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedText);

        byte[] iv = new byte[IV_SIZE];
        byte[] encrypted = new byte[combined.length - IV_SIZE];

        System.arraycopy(combined, 0, iv, 0, IV_SIZE);
        System.arraycopy(combined, IV_SIZE, encrypted, 0, encrypted.length);

        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        SecretKeySpec keySpec = new SecretKeySpec(getKeyBytes(), KEY_ALGORITHM);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * 암호화 키를 32바이트로 변환 (AES-256 요구사항)
     */
    private byte[] getKeyBytes() {
        byte[] keyBytes = new byte[32];
        byte[] secretKeyBytes = cryptoProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretKeyBytes, 0, keyBytes, 0, Math.min(secretKeyBytes.length, 32));
        return keyBytes;
    }
}

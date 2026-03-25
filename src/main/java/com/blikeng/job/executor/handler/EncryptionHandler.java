package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.exception.AlgorithmException;
import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.payloads.EncryptionPayload;
import com.blikeng.job.executor.payloads.FilePayload;
import com.blikeng.job.executor.service.StorageService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionHandler extends BaseHandler {
    protected EncryptionHandler(ObjectMapper objectMapper, StorageService storageService) {
        super(objectMapper, storageService);
    }

    public JsonNode handleFileEncryption(String payloadString) {
        EncryptionPayload payload = parsePayload(payloadString, EncryptionPayload.class, "File Encryption");

        Path path = getFilePath(payload.content(), "File encryption");

        try {
            byte[] plainText = Files.readAllBytes(path);
            EncryptionData data = encrypt(plainText, payload.key());

            return objectMapper.createObjectNode()
                    .put("algorithm", data.algorithm())
                    .put("cipherText", data.cipherText())
                    .put("iv", data.iv())
                    .put("key", data.key());

        } catch (IOException e) {
            throw new FileProcessingException("Failed to read file", "EncryptionHandler.handleFileEncryption", e);
        }
    }

    public JsonNode handleTextEncryption(String payloadString) {
        EncryptionPayload payload = parsePayload(payloadString, EncryptionPayload.class, "Text Encryption");

        String content = payload.content();
        if (content == null || content.isBlank()) {
            throw new InvalidPayloadException("Content must not be null or empty", "EncryptionHandler.handleTextEncryption", null);
        }

        byte[] plainText = content.getBytes(StandardCharsets.UTF_8);
        EncryptionData data = encrypt(plainText, payload.key());

        return objectMapper.createObjectNode()
                .put("algorithm", data.algorithm())
                .put("cipherText", data.cipherText())
                .put("iv", data.iv())
                .put("key", data.key());

    }

    private EncryptionData encrypt(byte[] plainText, String base64Key) {
        byte[] keyBytes = resolveAesKey(base64Key);
        SecretKey key = new SecretKeySpec(keyBytes, "AES");

        try {
            byte[] iv = generateRandomBytes(12);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] cipherText = cipher.doFinal(plainText);

            return new EncryptionData(
                    "AES/GCM/NoPadding",
                    Base64.getEncoder().encodeToString(cipherText),
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(keyBytes)
            );

        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new AlgorithmException(
                    "Algorithm not found",
                    "EncryptionHandler.encrypt",
                    "AES/GCM/NoPadding"
            );
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new InvalidPayloadException("Invalid encryption key or parameters", "EncryptionHandler.encrypt", e);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new FileProcessingException("Encryption failed", "EncryptionHandler.encrypt", e);
        }
    }

    private byte[] resolveAesKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            return generateRandomBytes(32);
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidPayloadException("Invalid Base64 key", "EncryptionHandler.resolveAesKey", e);
        }

        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new InvalidPayloadException("Invalid AES key length", "EncryptionHandler.resolveAesKey", null);
        }

        return keyBytes;
    }

    private byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private record EncryptionData(
            String algorithm,
            String cipherText,
            String iv,
            String key
    ) {}
}
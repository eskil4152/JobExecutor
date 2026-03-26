package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.exception.AlgorithmException;
import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.exception.messages.InternalMessages;
import com.blikeng.job.executor.payloads.DecryptionPayload;
import com.blikeng.job.executor.payloads.EncryptionPayload;
import com.blikeng.job.executor.service.StorageService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.*;
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
    public EncryptionHandler(ObjectMapper objectMapper, StorageService storageService) {
        super(objectMapper, storageService);
    }
    
    private final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";

    public JsonNode handleFileEncryption(String payloadString) {
        EncryptionPayload payload = parsePayload(payloadString, EncryptionPayload.class, "File Encryption");

        Path path = getFilePath(payload.fileId(), "File encryption");

        try {
            byte[] plainText = Files.readAllBytes(path);
            EncryptionData data = encrypt(plainText, payload.key());

            Path outputPath = path.resolveSibling(path.getFileName() + ".enc");
            Files.write(outputPath, data.cipherBytes());

            return objectMapper.createObjectNode()
                    .put("outputFile", outputPath.toString())
                    .put("algorithm", data.algorithm())
                    .put("iv", data.iv())
                    .put("key", data.key());

        } catch (IOException e) {
            throw new FileProcessingException(InternalMessages.FAILED_TO_READ_FILE.getMessage(), "EncryptionHandler.handleFileEncryption", e);
        }
    }

    public JsonNode handleFileDecryption(String payloadString) {
        DecryptionPayload payload = parsePayload(payloadString, DecryptionPayload.class, "File Decryption");

        if (payload.fileId() == null || payload.iv() == null || payload.key() == null) {
            throw new InvalidPayloadException(InternalMessages.INVALID_FILE_CRYPTO_PARAMS.getMessage(), "EncryptionHandler.handleFileDecryption", null);
        }

        Path inputPath = getFilePath(payload.fileId(), "File Decryption");

        try {
            byte[] cipherBytes = Files.readAllBytes(inputPath);
            byte[] plainBytes = decrypt(cipherBytes, payload.iv(), payload.key());

            Path outputPath = inputPath.resolveSibling(inputPath.getFileName() + ".decrypted");
            Files.write(outputPath, plainBytes);

            return objectMapper.createObjectNode()
                    .put("file_path", outputPath.getFileName().toString());

        } catch (IOException e) {
            throw new FileProcessingException(InternalMessages.FILE_READ_WRITE_FAILED.getMessage(), "EncryptionHandler.handleFileDecryption", e);
        }
    }

    public JsonNode handleTextEncryption(String payloadString) {
        EncryptionPayload payload = parsePayload(payloadString, EncryptionPayload.class, "Text Encryption");

        String content = payload.content();
        if (content == null || content.isBlank()) {
            throw new InvalidPayloadException(InternalMessages.INVALID_TEXT_CONTENT.getMessage(), "EncryptionHandler.handleTextEncryption", null);
        }

        byte[] plainText = content.getBytes(StandardCharsets.UTF_8);
        EncryptionData data = encrypt(plainText, payload.key());

        return objectMapper.createObjectNode()
                .put("algorithm", data.algorithm())
                .put("cipherText", Base64.getEncoder().encodeToString(data.cipherBytes()))
                .put("iv", data.iv())
                .put("key", data.key());

    }

    public JsonNode handleTextDecryption(String payloadString) {
        DecryptionPayload payload = parsePayload(payloadString, DecryptionPayload.class, "Text Decryption");

        if (payload.content() == null || payload.iv() == null || payload.key() == null) {
            throw new InvalidPayloadException(InternalMessages.INVALID_DECRYPTION_PARAMS.getMessage(), "EncryptionHandler.handleTextDecryption" , null);
        }

        byte[] cipherBytes = decodeBase64(payload.content(), "cipherText");
        byte[] plainBytes = decrypt(cipherBytes, payload.iv(), payload.key());
        String text = new String(plainBytes, StandardCharsets.UTF_8);

        return objectMapper.createObjectNode()
                .put("content", text);
    }

    private EncryptionData encrypt(byte[] plainText, String base64Key) {
        byte[] keyBytes = resolveAesKey(base64Key);
        SecretKey key = new SecretKeySpec(keyBytes, "AES");

        try {
            byte[] iv = generateRandomBytes(12);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] cipherBytes = cipher.doFinal(plainText);

            return new EncryptionData(
                    AES_GCM_NO_PADDING,
                    cipherBytes,
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(keyBytes)
            );

        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new AlgorithmException(
                    InternalMessages.ALGORITHM_NOT_FOUND.getMessage(),
                    "EncryptionHandler.encrypt",
                    AES_GCM_NO_PADDING,
                    e
            );
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new InvalidPayloadException(InternalMessages.INVALID_KEY_OR_PARAMETERS.getMessage(), "EncryptionHandler.encrypt", e);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new FileProcessingException(InternalMessages.ENCRYPTION_FAILED.getMessage(), "EncryptionHandler.encrypt", e);
        }
    }

    private byte[] decrypt(byte[] cipherBytes, String base64Iv, String base64Key) {
        byte[] iv = decodeBase64(base64Iv, "IV");
        byte[] keyBytes = decodeBase64(base64Key, "Key");

        if (!(keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32)) {
            throw new InvalidPayloadException(InternalMessages.INVALID_AES_KEY_LENGTH.getMessage(), "EncryptionHandler.decrypt", null);
        }

        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            return cipher.doFinal(cipherBytes);

        } catch (AEADBadTagException e) {
            throw new FileProcessingException(
                    InternalMessages.DECRYPTION_FAILED_AUTH_ERROR.getMessage(),
                    "EncryptionHandler.decrypt",
                    e
            );

        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new AlgorithmException(
                    InternalMessages.ALGORITHM_NOT_FOUND.getMessage(),
                    "EncryptionHandler.decrypt",
                    AES_GCM_NO_PADDING,
                    e
            );

        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new InvalidPayloadException(InternalMessages.INVALID_KEY_OR_PARAMETERS.getMessage(), "EncryptionHandler.decrypt", e);

        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new FileProcessingException(InternalMessages.DECRYPTION_FAILED.getMessage(), "EncryptionHandler.decrypt", e);
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
            throw new InvalidPayloadException(InternalMessages.INVALID_BASE64.getMessage(), "EncryptionHandler.resolveAesKey", e);
        }

        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new InvalidPayloadException(InternalMessages.INVALID_AES_KEY_LENGTH.getMessage(), "EncryptionHandler.resolveAesKey", null);
        }

        return keyBytes;
    }

    private byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private byte[] decodeBase64(String value, String field) {
        try {
            return Base64.getDecoder().decode(value.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidPayloadException(InternalMessages.INVALID_BASE64.getMessage(), "EncryptionHandler.decodeBase64: " + field, e);
        }
    }

    private record EncryptionData(
            String algorithm,
            byte[] cipherBytes,
            String iv,
            String key
    ) {}
}
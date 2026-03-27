package com.blikeng.job.executor.unitTests.handlerTests;

import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.handler.EncryptionHandler;
import com.blikeng.job.executor.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncryptionHandlerTests {
    // ==========================
    // Tests for EncryptionHandler. Verifies:
    // - Text encryption returns ciphertext, iv, key, algorithm
    // - Text decryption round-trips correctly
    // - Null/blank content throws InvalidPayloadException
    // - Null key generates a random key
    // - AES-128 and AES-192 key lengths encrypt/decrypt correctly
    // - Invalid base64 key throws InvalidPayloadException
    // - Wrong key length throws InvalidPayloadException
    // - Wrong key/iv on decryption throws FileProcessingException
    // - File encryption creates .enc file
    // - File decryption creates .decrypted file
    // - Null fileId/iv/key on file decryption throws InvalidPayloadException each
    // - Missing file on decryption throws FileProcessingException
    // Note: NoSuchAlgorithm, InvalidKey, IllegalBlockSize catches in encrypt/decrypt
    //       are dead code — AES/GCM/NoPadding is guaranteed on any standard JVM
    // ==========================

    @TempDir Path tempDir;
    @Mock private StorageService storageService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private EncryptionHandler encryptionHandler;

    @BeforeEach
    void setUp() {
        encryptionHandler = new EncryptionHandler(objectMapper, storageService);
    }

    private static String validKey(int bytes) {
        byte[] keyBytes = new byte[bytes];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    private static String validAes256Key() { return validKey(32); }

    // ==========================
    // Handle Text Encryption
    // ==========================
    @Test
    void shouldEncryptTextAndReturnRequiredFields() {
        String key = validAes256Key();

        JsonNode result = encryptionHandler.handleTextEncryption(
                "{\"content\": \"hello world\", \"key\": \"" + key + "\"}");

        assertThat(result.get("cipherText")).isNotNull();
        assertThat(result.get("iv")).isNotNull();
        assertThat(result.get("key")).isNotNull();
        assertThat(result.get("algorithm").asString()).isEqualTo("AES/GCM/NoPadding");
    }

    @Test
    void shouldGenerateRandomKeyWhenKeyIsNull() {
        JsonNode result = encryptionHandler.handleTextEncryption("""
                {
                  "content": "hello"
                }
                """);

        String generatedKey = result.get("key").asString();
        assertThat(generatedKey).isNotBlank();
        assertThatCode(() -> Base64.getDecoder().decode(generatedKey)).doesNotThrowAnyException();
    }

    @Test
    void shouldGenerateRandomKeyWhenKeyIsBlank() {
        JsonNode result = encryptionHandler.handleTextEncryption("""
                {
                  "content": "hello",
                  "key": ""
                }
                """);

        assertThat(result.get("key").asString()).isNotBlank();
    }

    @Test
    void shouldThrowInvalidPayloadForNullContent() {
        String key = validAes256Key();
        assertThatThrownBy(() -> encryptionHandler.handleTextEncryption(
                "{\"content\": null, \"key\": \"" + key + "\"}"))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowInvalidPayloadForBlankContent() {
        String key = validAes256Key();
        assertThatThrownBy(() -> encryptionHandler.handleTextEncryption(
                "{\"content\": \"   \", \"key\": \"" + key + "\"}"))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowInvalidPayloadForInvalidBase64Key() {
        assertThatThrownBy(() -> encryptionHandler.handleTextEncryption("""
                {
                  "content": "hello",
                  "key": "not-base64!!!"
                }
                """))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowInvalidPayloadForInvalidKeyLength() {
        byte[] shortKey = new byte[15];
        new SecureRandom().nextBytes(shortKey);
        String invalidKey = Base64.getEncoder().encodeToString(shortKey);

        assertThatThrownBy(() -> encryptionHandler.handleTextEncryption(
                "{\"content\": \"hello\", \"key\": \"" + invalidKey + "\"}"))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldEncryptAndDecryptWithAes128Key() {
        String key = validKey(16);

        JsonNode encrypted = encryptionHandler.handleTextEncryption(
                "{\"content\": \"hello\", \"key\": \"" + key + "\"}");

        String cipherText = encrypted.get("cipherText").asString();
        String iv = encrypted.get("iv").asString();
        String usedKey = encrypted.get("key").asString();

        JsonNode result = encryptionHandler.handleTextDecryption(
                "{\"content\": \"" + cipherText + "\", \"iv\": \"" + iv + "\", \"key\": \"" + usedKey + "\"}");

        assertThat(result.get("content").asString()).isEqualTo("hello");
    }

    @Test
    void shouldEncryptAndDecryptWithAes192Key() {
        String key = validKey(24);

        JsonNode encrypted = encryptionHandler.handleTextEncryption(
                "{\"content\": \"hello\", \"key\": \"" + key + "\"}");

        String cipherText = encrypted.get("cipherText").asString();
        String iv = encrypted.get("iv").asString();
        String usedKey = encrypted.get("key").asString();

        JsonNode result = encryptionHandler.handleTextDecryption(
                "{\"content\": \"" + cipherText + "\", \"iv\": \"" + iv + "\", \"key\": \"" + usedKey + "\"}");

        assertThat(result.get("content").asString()).isEqualTo("hello");
    }

    // ==========================
    // Handle Text Decryption
    // ==========================
    @Test
    void shouldDecryptTextRoundTrip() {
        String key = validAes256Key();

        JsonNode encrypted = encryptionHandler.handleTextEncryption(
                "{\"content\": \"secret message\", \"key\": \"" + key + "\"}");

        String cipherText = encrypted.get("cipherText").asString();
        String iv = encrypted.get("iv").asString();
        String usedKey = encrypted.get("key").asString();

        JsonNode result = encryptionHandler.handleTextDecryption(
                "{\"content\": \"" + cipherText + "\", \"iv\": \"" + iv + "\", \"key\": \"" + usedKey + "\"}");

        assertThat(result.get("content").asString()).isEqualTo("secret message");
    }

    @ParameterizedTest(name = "{index} → {0}")
    @MethodSource("invalidDecryptionPayloads")
    void shouldThrowInvalidPayloadForInvalidInputs(String payload) {
        assertThatThrownBy(() ->
                encryptionHandler.handleTextDecryption(payload)
        ).isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowFileProcessingExceptionForWrongKey() {
        String keyA = validAes256Key();
        String keyB = validAes256Key();

        JsonNode encrypted = encryptionHandler.handleTextEncryption(
                "{\"content\": \"hello\", \"key\": \"" + keyA + "\"}");

        String cipherText = encrypted.get("cipherText").asString();
        String iv = encrypted.get("iv").asString();

        assertThatThrownBy(() -> encryptionHandler.handleTextDecryption(
                "{\"content\": \"" + cipherText + "\", \"iv\": \"" + iv + "\", \"key\": \"" + keyB + "\"}"))
                .isInstanceOf(FileProcessingException.class);
    }

    @Test
    void shouldThrowInvalidPayloadForInvalidDecryptionKeyLength() {
        byte[] shortKey = new byte[15];
        new SecureRandom().nextBytes(shortKey);
        String invalidKey = Base64.getEncoder().encodeToString(shortKey);

        assertThatThrownBy(() -> encryptionHandler.handleTextDecryption(
                "{\"content\": \"aGVsbG8=\", \"iv\": \"aGVsbG8=\", \"key\": \"" + invalidKey + "\"}"))
                .isInstanceOf(InvalidPayloadException.class);
    }

    // ==========================
    // Handle File Encryption
    // ==========================
    @Test
    void shouldEncryptFileAndCreateEncFile() throws Exception {
        Path file = tempDir.resolve("plain.txt");
        Files.writeString(file, "hello world");
        when(storageService.getPath(anyString())).thenReturn(file);

        JsonNode result = encryptionHandler.handleFileEncryption("""
                {
                  "fileId": "plain-id"
                }
                """);

        assertThat(result.get("outputFile")).isNotNull();
        assertThat(result.get("iv")).isNotNull();
        assertThat(result.get("key")).isNotNull();
        assertThat(result.get("algorithm").asString()).isEqualTo("AES/GCM/NoPadding");
        assertThat(Path.of(result.get("outputFile").asString())).exists();
    }

    @Test
    void shouldThrowFileProcessingExceptionForMissingFileOnEncryption() {
        Path nonExistent = tempDir.resolve("missing.txt");
        when(storageService.getPath(anyString())).thenReturn(nonExistent);

        assertThatThrownBy(() -> encryptionHandler.handleFileEncryption("""
                {
                  "fileId": "missing-id"
                }
                """))
                .isInstanceOf(FileProcessingException.class);
    }

    // ==========================
    // Handle File Decryption
    // ==========================
    @Test
    void shouldDecryptFileRoundTrip() throws Exception {
        Path file = tempDir.resolve("plain.txt");
        Files.writeString(file, "hello world");
        when(storageService.getPath("plain-id")).thenReturn(file);

        JsonNode encrypted = encryptionHandler.handleFileEncryption("""
                {
                  "fileId": "plain-id"
                }
                """);

        String iv = encrypted.get("iv").asString();
        String key = encrypted.get("key").asString();
        Path encFile = Path.of(encrypted.get("outputFile").asString());

        when(storageService.getPath("enc-id")).thenReturn(encFile);

        JsonNode result = encryptionHandler.handleFileDecryption(
                "{\"fileId\": \"enc-id\", \"iv\": \"" + iv + "\", \"key\": \"" + key + "\"}");

        Path decrypted = encFile.resolveSibling(result.get("file_path").asString());
        assertThat(Files.readString(decrypted)).isEqualTo("hello world");
    }

    @Test
    void shouldThrowInvalidPayloadWhenFileIdIsNull() {
        String key = validAes256Key();

        assertThatThrownBy(() -> encryptionHandler.handleFileDecryption(
                "{\"fileId\": null, \"iv\": \"aGVsbG8=\", \"key\": \"" + key + "\"}"))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowInvalidPayloadWhenFileDecryptionIvIsNull() {
        String key = validAes256Key();

        assertThatThrownBy(() -> encryptionHandler.handleFileDecryption(
                "{\"fileId\": \"aGVsbG8=\", \"iv\": null, \"key\": \"" + key + "\"}"))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowInvalidPayloadWhenFileDecryptionKeyIsNull() {
        assertThatThrownBy(() -> encryptionHandler.handleFileDecryption(
                "{\"fileId\": \"aGVsbG8=\", \"iv\": \"aGVsbG8=\", \"key\": null}"))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowFileProcessingExceptionForMissingFileOnDecryption() {
        String key = validAes256Key();

        Path nonExistent = tempDir.resolve("missing.enc");
        when(storageService.getPath(anyString())).thenReturn(nonExistent);

        assertThatThrownBy(() -> encryptionHandler.handleFileDecryption(
                "{\"fileId\": \"enc-id\", \"iv\": \"aGVsbG8=\", \"key\": \"" + key + "\"}"))
                .isInstanceOf(FileProcessingException.class);
    }

    @Test
    void shouldThrowInvalidPayloadForInvalidBase64KeyOnFileDecryption() throws Exception {
        Path file = tempDir.resolve("some.enc");
        Files.write(file, new byte[]{1, 2, 3});
        when(storageService.getPath(anyString())).thenReturn(file);

        assertThatThrownBy(() -> encryptionHandler.handleFileDecryption(
                "{\"fileId\": \"enc-id\", \"iv\": \"aGVsbG8=\", \"key\": \"not-base64!!!\"}"))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowInvalidPayloadForInvalidKeyLengthOnFileDecryption() throws Exception {
        Path file = tempDir.resolve("some.enc");
        Files.write(file, new byte[]{1, 2, 3});
        when(storageService.getPath(anyString())).thenReturn(file);

        String shortKey = Base64.getEncoder().encodeToString(new byte[15]);

        assertThatThrownBy(() -> encryptionHandler.handleFileDecryption(
                "{\"fileId\": \"enc-id\", \"iv\": \"aGVsbG8=\", \"key\": \"" + shortKey + "\"}"))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowFileProcessingExceptionForWrongKeyOnFileDecryption() throws Exception {
        String key = validAes256Key();
        Path file = tempDir.resolve("plain.txt");
        Files.writeString(file, "hello");
        when(storageService.getPath("plain-id")).thenReturn(file);

        JsonNode encrypted = encryptionHandler.handleFileEncryption("""
                {
                  "fileId": "plain-id"
                }
                """);

        String iv = encrypted.get("iv").asString();
        Path encFile = Path.of(encrypted.get("outputFile").asString());
        when(storageService.getPath("enc-id")).thenReturn(encFile);

        assertThatThrownBy(() -> encryptionHandler.handleFileDecryption(
                "{\"fileId\": \"enc-id\", \"iv\": \"" + iv + "\", \"key\": \"" + key + "\"}"))
                .isInstanceOf(FileProcessingException.class);
    }

    private static Stream<String> invalidDecryptionPayloads() {
        String key = validAes256Key();

        return Stream.of(
                "{\"content\": null, \"iv\": \"aGVsbG8=\", \"key\": \"" + key + "\"}",
                "{\"content\": \"aGVsbG8=\", \"iv\": null, \"key\": \"" + key + "\"}",
                "{\"content\": \"not-base64!!!\", \"iv\": \"aGVsbG8=\", \"key\": \"" + key + "\"}",
                """
                {
                  "content": "aGVsbG8=",
                  "iv": "aGVsbG8=",
                  "key": null
                }
                """
        );
    }
}

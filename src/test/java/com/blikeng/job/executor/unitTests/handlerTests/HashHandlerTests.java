package com.blikeng.job.executor.unitTests.handlerTests;

import com.blikeng.job.executor.exception.AlgorithmException;
import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.handler.HashHandler;
import com.blikeng.job.executor.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HashHandlerTests {
    // ==========================
    // Tests for HashHandler. Verifies:
    // - Text hashing returns correct hex hash
    // - Null/blank algorithm defaults to SHA-256
    // - Invalid algorithm throws AlgorithmException
    // - Null/blank content throws InvalidPayloadException
    // - File hashing reads file and returns correct hash
    // - Hash comparison returns true/false correctly
    // - Null hashes in comparison throw InvalidPayloadException
    // ==========================

    @TempDir Path tempDir;
    @Mock private StorageService storageService;

    private ObjectMapper objectMapper;
    private HashHandler hashHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        hashHandler = new HashHandler(objectMapper, storageService);
    }

    // ==========================
    // handleTextHashing
    // ==========================
    @Test
    void shouldReturnCorrectSha256HashForText() throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("hello".getBytes(StandardCharsets.UTF_8));
        String expectedHash = HexFormat.of().formatHex(digest);

        JsonNode result = hashHandler.handleTextHashing("""
                {
                  "content": "hello",
                  "algorithm": "SHA-256"
                }
                """);

        assertThat(result.get("hash").asString()).isEqualTo(expectedHash);
        assertThat(result.get("algorithm").asString()).isEqualTo("SHA-256");
    }

    @Test
    void shouldReturnCorrectHashForTextWithCustomAlgorithm() throws Exception {
        byte[] digest = MessageDigest.getInstance("MD5").digest("hello".getBytes(StandardCharsets.UTF_8));
        String expectedHash = HexFormat.of().formatHex(digest);

        JsonNode result = hashHandler.handleTextHashing("""
                {
                  "content": "hello",
                  "algorithm": "MD5"
                }
                """);

        assertThat(result.get("hash").asString()).isEqualTo(expectedHash);
        assertThat(result.get("algorithm").asString()).isEqualTo("MD5");
    }

    @Test
    void shouldDefaultToSha256WhenAlgorithmIsBlank() {
        JsonNode result = hashHandler.handleTextHashing("""
                {
                  "content": "hello",
                  "algorithm": ""
                }
                """);

        assertThat(result.get("algorithm").asString()).isEqualTo("SHA-256");
    }

    @Test
    void shouldDefaultToSha256WhenAlgorithmIsNull() {
        JsonNode payload = objectMapper.createObjectNode().put("content", "hello");

        JsonNode result = hashHandler.handleTextHashing(objectMapper.writeValueAsString(payload));

        assertThat(result.get("algorithm").asString()).isEqualTo("SHA-256");
    }

    @Test
    void shouldThrowAlgorithmExceptionForInvalidAlgorithm() {
        assertThatThrownBy(() -> hashHandler.handleTextHashing("""
                {
                  "content": "hello",
                  "algorithm": "INVALID"
                }
                """))
                .isInstanceOf(AlgorithmException.class);
    }

    @Test
    void shouldThrowInvalidPayloadForNullTextContent() {
        assertThatThrownBy(() -> hashHandler.handleTextHashing("""
                {
                  "content": null,
                  "algorithm": "SHA-256"
                }
                """))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowInvalidPayloadForBlankTextContent() {
        assertThatThrownBy(() -> hashHandler.handleTextHashing("""
                {
                  "content": "   ",
                  "algorithm": "SHA-256"
                }
                """))
                .isInstanceOf(InvalidPayloadException.class);
    }

    // ==========================
    // handleFileHashing
    // ==========================
    @Test
    void shouldReturnCorrectHashForFileWithCustomAlgorithm() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");
        when(storageService.getPath(anyString())).thenReturn(file);

        byte[] digest = MessageDigest.getInstance("SHA-256").digest("hello".getBytes(StandardCharsets.UTF_8));
        String expectedHash = HexFormat.of().formatHex(digest);

        JsonNode payload = objectMapper.createObjectNode()
                .put("fileId", "test-id")
                .put("algorithm", "SHA-256");

        JsonNode result = hashHandler.handleFileHashing(objectMapper.writeValueAsString(payload));

        assertThat(result.get("hash").asString()).isEqualTo(expectedHash);
    }

    @Test
    void shouldReturnCorrectHashForFileWithFallbackAlgorithmWhenAlgorithmIsNull() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");
        when(storageService.getPath(anyString())).thenReturn(file);

        byte[] digest = MessageDigest.getInstance("SHA-256").digest("hello".getBytes(StandardCharsets.UTF_8));
        String expectedHash = HexFormat.of().formatHex(digest);

        JsonNode payload = objectMapper.createObjectNode()
                .put("fileId", "test-id");

        JsonNode result = hashHandler.handleFileHashing(objectMapper.writeValueAsString(payload));

        assertThat(result.get("hash").asString()).isEqualTo(expectedHash);
    }

    @Test
    void shouldReturnCorrectHashForFileWithFallbackAlgorithmWhenAlgorithmIsBlank() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");
        when(storageService.getPath(anyString())).thenReturn(file);

        byte[] digest = MessageDigest.getInstance("SHA-256").digest("hello".getBytes(StandardCharsets.UTF_8));
        String expectedHash = HexFormat.of().formatHex(digest);

        JsonNode payload = objectMapper.createObjectNode()
                .put("fileId", "test-id")
                .put("algorithm", "");

        JsonNode result = hashHandler.handleFileHashing(objectMapper.writeValueAsString(payload));

        assertThat(result.get("hash").asString()).isEqualTo(expectedHash);
    }

    @Test
    void shouldThrowFileProcessingExceptionForMissingFile() {
        Path nonExistent = tempDir.resolve("missing.txt");
        when(storageService.getPath(anyString())).thenReturn(nonExistent);

        assertThatThrownBy(() -> hashHandler.handleFileHashing("""
                {
                  "fileId": "missing-id",
                  "algorithm": "SHA-256"
                }
                """))
                .isInstanceOf(FileProcessingException.class);
    }

    @Test
    void shouldThrowAlgorithmExceptionForInvalidFileHashAlgorithm() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");
        when(storageService.getPath(anyString())).thenReturn(file);

        assertThatThrownBy(() -> hashHandler.handleFileHashing("""
                {
                  "fileId": "test-id",
                  "algorithm": "INVALID"
                }
                """))
                .isInstanceOf(AlgorithmException.class);
    }

    // ==========================
    // handleHashComparison
    // ==========================
    @Test
    void shouldReturnTrueForMatchingHashes() {
        JsonNode result = hashHandler.handleHashComparison("""
                {
                  "hashA": "abc",
                  "hashB": "abc"
                }
                """);

        assertThat(result.get("match").asBoolean()).isTrue();
    }

    @Test
    void shouldReturnFalseForNonMatchingHashes() {
        JsonNode result = hashHandler.handleHashComparison("""
                {
                  "hashA": "abc",
                  "hashB": "xyz"
                }
                """);

        assertThat(result.get("match").asBoolean()).isFalse();
    }

    @Test
    void shouldThrowInvalidPayloadWhenHashAIsNullOrBlank() {
        assertThatThrownBy(() -> hashHandler.handleHashComparison("""
                {
                  "hashA": null,
                  "hashB": "abc"
                }
                """))
                .isInstanceOf(InvalidPayloadException.class);

        assertThatThrownBy(() -> hashHandler.handleHashComparison("""
                {
                  "hashA": "",
                  "hashB": "abc"
                }
                """))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowInvalidPayloadWhenHashBIsNullOrBlank() {
        assertThatThrownBy(() -> hashHandler.handleHashComparison("""
                {
                  "hashA": "abc",
                  "hashB": null
                }
                """))
                .isInstanceOf(InvalidPayloadException.class);

        assertThatThrownBy(() -> hashHandler.handleHashComparison("""
                {
                  "hashA": "abc",
                  "hashB": ""
                }
                """))
                .isInstanceOf(InvalidPayloadException.class);
    }
}
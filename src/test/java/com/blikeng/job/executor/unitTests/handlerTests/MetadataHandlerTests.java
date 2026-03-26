package com.blikeng.job.executor.unitTests.handlerTests;

import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.exception.MetadataException;
import com.blikeng.job.executor.handler.MetadataHandler;
import com.blikeng.job.executor.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MetadataHandlerTests {
    // ==========================
    // Tests for MetadataHandler. Verifies:
    // - Returns general metadata fields (name, size, extension, mimeType)
    // - File type is correctly detected for known formats
    // - Missing file throws InvalidPayloadException
    // Note: uses real Tika — tests require actual files in tempDir
    // ==========================

    @TempDir Path tempDir;
    @Mock private StorageService storageService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MetadataHandler metadataHandler;

    @BeforeEach
    void setUp() {
        metadataHandler = new MetadataHandler(objectMapper, storageService);
    }

    // ==========================
    // Handle MetadataExtraction
    // ==========================
    @Test
    void shouldReturnGeneralMetadataForTextFile() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        when(storageService.getPath(any())).thenReturn(file);

        JsonNode result = metadataHandler.handleMetadataExtraction("""
            {
              "fileId": "test-id"
            }
            """);

        assertThat(result.get("name").asString()).isEqualTo("test.txt");
        assertThat(result.get("size").asLong()).isEqualTo(Files.size(file));
        assertThat(result.get("Content-Type").asString()).startsWith("text/plain");
    }

    @Test
    void shouldDetectMimeTypeForTextFile() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        when(storageService.getPath(any())).thenReturn(file);

        JsonNode result = metadataHandler.handleMetadataExtraction("""
            {
              "fileId": "test-id"
            }
            """);

        System.out.println(result.toPrettyString());

        assertThat(result.get("Content-Type").asString()).startsWith("text/plain");
    }

    @Test
    void shouldThrowInvalidPayloadForMissingFile() {
        Path nonExistent = tempDir.resolve("missing.txt");
        when(storageService.getPath(any())).thenReturn(nonExistent);

        assertThatThrownBy(() -> metadataHandler.handleMetadataExtraction("""
            {
              "fileId": "not even a real id"
            }
            """))
                .isInstanceOf(MetadataException.class);
    }
}
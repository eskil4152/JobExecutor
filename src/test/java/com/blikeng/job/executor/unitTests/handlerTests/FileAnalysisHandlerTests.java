package com.blikeng.job.executor.unitTests.handlerTests;

import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.handler.FileAnalysisHandler;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileAnalysisHandlerTests {
    // ==========================
    // Tests for FileAnalysisHandler. Verifies:
    // - Returns correct word, line, character, and byte counts
    // - Empty file returns all zeros
    // - Unreadable/missing file throws FileProcessingException
    // - Multi-line content produces correct line count
    // ==========================

    @TempDir Path tempDir;
    @Mock private StorageService storageService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FileAnalysisHandler fileAnalysisHandler;

    @BeforeEach
    void setUp() {
        fileAnalysisHandler = new FileAnalysisHandler(objectMapper, storageService);
    }

    // ==========================
    // Handle FileAnalysis
    // ==========================
    @Test
    void shouldReturnCorrectCountsForFile() throws Exception {
        String content = "hello world\nfoo bar";
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, content);
        when(storageService.getPath(anyString())).thenReturn(file);

        JsonNode result = fileAnalysisHandler.handleFileAnalysis("""
                {
                  "fileId": "test-id"
                }
                """);

        assertThat(result.get("words").asInt()).isEqualTo(4);
        assertThat(result.get("lines").asInt()).isEqualTo(2);
        assertThat(result.get("characters").asInt()).isEqualTo(content.length());
        assertThat(result.get("bytes").asInt()).isEqualTo(content.getBytes().length);
    }

    @Test
    void shouldReturnZeroCountsForEmptyFile() throws Exception {
        Path file = tempDir.resolve("empty.txt");
        Files.writeString(file, "");
        when(storageService.getPath(anyString())).thenReturn(file);

        JsonNode result = fileAnalysisHandler.handleFileAnalysis("""
                {
                  "fileId": "empty-id"
                }
                """);

        assertThat(result.get("words").asInt()).isZero();
        assertThat(result.get("lines").asInt()).isZero();
        assertThat(result.get("characters").asInt()).isZero();
        assertThat(result.get("bytes").asInt()).isZero();
    }

    @Test
    void shouldReturnCorrectLineCountForMultiLineFile() throws Exception {
        Path file = tempDir.resolve("multiline.txt");
        Files.writeString(file, "line1\nline2\nline3\nline4\nline5");
        when(storageService.getPath(anyString())).thenReturn(file);

        JsonNode result = fileAnalysisHandler.handleFileAnalysis("""
                {
                  "fileId": "multi-id"
                }
                """);

        assertThat(result.get("lines").asInt()).isEqualTo(5);
    }

    @Test
    void shouldThrowFileProcessingExceptionForMissingFile() {
        Path nonExistent = tempDir.resolve("missing.txt");
        when(storageService.getPath(anyString())).thenReturn(nonExistent);

        assertThatThrownBy(() -> fileAnalysisHandler.handleFileAnalysis("""
                {
                  "fileId": "missing-id"
                }
                """))
                .isInstanceOf(FileProcessingException.class);
    }
}
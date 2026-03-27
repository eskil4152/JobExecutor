package com.blikeng.job.executor.unitTests.handlerTests;

import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.handler.CompressionHandler;
import com.blikeng.job.executor.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompressionHandlerTests {
    // ==========================
    // Tests for CompressionHandler. Verifies:
    // - Text compression returns valid base64 zip
    // - Text decompression round-trips correctly
    // - Null/blank content throws appropriate exceptions
    // - Invalid base64 input throws InvalidPayloadException
    // - Empty zip throws FileProcessingException
    // - Corrupt (non-zip) base64 content throws FileProcessingException
    // - File compression creates a zip file
    // - File decompression extracts correctly
    // - Missing zip file throws FileProcessingException
    // - Zip with directory entry throws FileProcessingException
    // - Zip with path traversal entry throws FileProcessingException
    // - Zip with multiple entries throws FileProcessingException
    // ==========================

    @TempDir Path tempDir;
    @Mock private StorageService storageService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CompressionHandler compressionHandler;

    @BeforeEach
    void setUp() {
        compressionHandler = new CompressionHandler(objectMapper, storageService);
        ReflectionTestUtils.setField(compressionHandler, "maxDecompressedSize", DataSize.ofGigabytes(2));
        ReflectionTestUtils.setField(compressionHandler, "maxExpansionRatio", 100);
    }

    // ==========================
    // Handle Text Compression
    // ==========================
    @Test
    void shouldCompressTextAndReturnBase64() {
        JsonNode result = compressionHandler.handleTextCompression("""
                {
                  "content": "hello world"
                }
                """);

        String compressed = result.get("compressed_text").asString();
        assertThat(compressed).isNotBlank();
        assertThatCode(() -> Base64.getDecoder().decode(compressed)).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowFileProcessingExceptionForNullTextContent() {
        assertThatThrownBy(() -> compressionHandler.handleTextCompression("""
                {
                  "content": null
                }
                """))
                .isInstanceOf(FileProcessingException.class);
    }

    @Test
    void shouldThrowFileProcessingExceptionForBlankTextContent() {
        assertThatThrownBy(() -> compressionHandler.handleTextCompression("""
                {
                  "content": "   "
                }
                """))
                .isInstanceOf(FileProcessingException.class);
    }

    // ==========================
    // Handle Text Decompression
    // ==========================
    @Test
    void shouldDecompressTextRoundTrip() {
        JsonNode compressed = compressionHandler.handleTextCompression("""
                {
                  "content": "hello world"
                }
                """);

        String compressedText = compressed.get("compressed_text").asString();

        JsonNode result = compressionHandler.handleTextDecompression(
                "{\"content\": \"" + compressedText + "\"}");

        assertThat(result.get("text").asString()).isEqualTo("hello world");
    }

    @Test
    void shouldThrowInvalidPayloadForNullCompressedContent() {
        assertThatThrownBy(() -> compressionHandler.handleTextDecompression("""
                {
                  "content": null
                }
                """))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowInvalidPayloadForInvalidBase64Content() {
        assertThatThrownBy(() -> compressionHandler.handleTextDecompression("""
                {
                  "content": "not-valid-base64!!!"
                }
                """))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowFileProcessingExceptionForCorruptZipContent() {
        String notAZip = Base64.getEncoder().encodeToString("this is not a zip".getBytes());

        assertThatThrownBy(() -> compressionHandler.handleTextDecompression(
                "{\"content\": \"" + notAZip + "\"}"))
                .isInstanceOf(FileProcessingException.class);
    }

    @Test
    void shouldThrowFileProcessingExceptionForEmptyZip() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ZipOutputStream(out).close();
        String emptyZipBase64 = Base64.getEncoder().encodeToString(out.toByteArray());

        assertThatThrownBy(() -> compressionHandler.handleTextDecompression(
                "{\"content\": \"" + emptyZipBase64 + "\"}"))
                .isInstanceOf(FileProcessingException.class);
    }

    // ==========================
    // Handle File Compression
    // ==========================
    @Test
    void shouldCompressFileAndReturnZipPath() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");
        when(storageService.getPath(anyString())).thenReturn(file);

        JsonNode result = compressionHandler.handleFileCompression("""
                {
                  "fileId": "test-id"
                }
                """);

        assertThat(result.get("file_path").asString()).endsWith(".zip");
        assertThat(tempDir.resolve(result.get("file_path").asString())).exists();
    }

    @Test
    void shouldThrowFileProcessingExceptionForMissingFile() {
        Path nonExistent = tempDir.resolve("missing.txt");
        when(storageService.getPath(anyString())).thenReturn(nonExistent);

        assertThatThrownBy(() -> compressionHandler.handleFileCompression("""
                {
                  "fileId": "missing-id"
                }
                """))
                .isInstanceOf(FileProcessingException.class);
    }

    // ==========================
    // Handle File Decompression
    // ==========================
    @Test
    void shouldThrowFileProcessingExceptionForMissingZipFile() {
        Path nonExistent = tempDir.resolve("missing.zip");
        when(storageService.getPath(anyString())).thenReturn(nonExistent);

        assertThatThrownBy(() -> compressionHandler.handleFileDecompression("""
                {
                  "fileId": "missing-id"
                }
                """))
                .isInstanceOf(FileProcessingException.class);
    }

    @Test
    void shouldDecompressFileCorrectly() throws Exception {
        Path zipFile = tempDir.resolve("test.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("test.txt"));
            zos.write("hello world".getBytes());
            zos.closeEntry();
        }
        when(storageService.getPath(anyString())).thenReturn(zipFile);

        JsonNode result = compressionHandler.handleFileDecompression("""
                {
                  "fileId": "zip-id"
                }
                """);

        Path decompressed = tempDir.resolve(result.get("decompressed_file").asString());
        assertThat(decompressed).exists();
        assertThat(Files.readString(decompressed)).isEqualTo("hello world");
    }

    @Test
    void shouldThrowFileProcessingExceptionForEmptyZipFile() throws Exception {
        Path zipFile = tempDir.resolve("empty.zip");
        new ZipOutputStream(Files.newOutputStream(zipFile)).close();
        when(storageService.getPath(anyString())).thenReturn(zipFile);

        assertThatThrownBy(() -> compressionHandler.handleFileDecompression("""
                {
                  "fileId": "empty-id"
                }
                """))
                .isInstanceOf(FileProcessingException.class);
    }

    @Test
    void shouldThrowFileProcessingExceptionForZipWithDirectoryEntry() throws Exception {
        Path zipFile = tempDir.resolve("dir.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("somedir/"));
            zos.closeEntry();
        }
        when(storageService.getPath(anyString())).thenReturn(zipFile);

        assertThatThrownBy(() -> compressionHandler.handleFileDecompression("""
                {
                  "fileId": "dir-id"
                }
                """))
                .isInstanceOf(FileProcessingException.class);
    }

    @Test
    void shouldThrowFileProcessingExceptionForZipWithPathTraversal() throws Exception {
        Path zipFile = tempDir.resolve("traversal.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("../../evil.txt"));
            zos.write("evil".getBytes());
            zos.closeEntry();
        }
        when(storageService.getPath(anyString())).thenReturn(zipFile);

        assertThatThrownBy(() -> compressionHandler.handleFileDecompression("""
                {
                  "fileId": "traversal-id"
                }
                """))
                .isInstanceOf(FileProcessingException.class);
    }

    @Test
    void shouldThrowFileProcessingExceptionForZipWithMultipleEntries() throws Exception {
        Path zipFile = tempDir.resolve("multi.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("file1.txt"));
            zos.write("one".getBytes());
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("file2.txt"));
            zos.write("two".getBytes());
            zos.closeEntry();
        }
        when(storageService.getPath(anyString())).thenReturn(zipFile);

        assertThatThrownBy(() -> compressionHandler.handleFileDecompression("""
                {
                  "fileId": "multi-id"
                }
                """))
                .isInstanceOf(FileProcessingException.class);
    }
}

package com.blikeng.job.executor.unitTests.serviceTests;

import com.blikeng.job.executor.exception.http.ApiException;
import com.blikeng.job.executor.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class StorageServiceTests {
    // ==========================
    // Tests for StorageService. Verifies:
    // - Uploaded file is stored and retrievable by returned ID
    // - Stored file content matches original
    // - Requesting unknown file ID throws 404
    // - Requesting malformed file ID throws 400
    // ==========================

    @TempDir
    Path tempDir;

    private StorageService storageService;

    @BeforeEach
    void setUp() throws Exception {
        storageService = new StorageService(tempDir.toString());
    }

    // ==========================
    // Store files
    // ==========================
    @Test
    void shouldStoreFileAndReturnId() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, world!".getBytes());

        String id = storageService.store(file);

        assertNotNull(id);
        assert tempDir.resolve(id).toFile().exists();
    }

    @Test
    void shouldStoreFileWithCorrectContent() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, world!".getBytes());

        String id = storageService.store(file);

        byte[] content = storageService.getFile(id).getInputStream().readAllBytes();
        assertArrayEquals("Hello, world!".getBytes(), content);
    }

    // ==========================
    // File Retrieval
    // ==========================
    @Test
    void shouldReturnResourceForStoredFile() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, world!".getBytes());

        String id = storageService.store(file);

        Resource resource = storageService.getFile(id);
        assertNotNull(resource);
    }

    @Test
    void shouldThrowNotFoundForUnknownFileId() {
        String id = UUID.randomUUID().toString();
        assertThatThrownBy(() -> storageService.getFile(id))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void shouldThrowBadRequestForInvalidFileId() {
        assertThatThrownBy(() -> storageService.getFile("../../foo.bar"))
                .isInstanceOf(ApiException.class);
    }
}

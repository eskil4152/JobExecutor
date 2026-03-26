package com.blikeng.job.executor.unitTests.controllerTests;

import com.blikeng.job.executor.controller.FileController;
import com.blikeng.job.executor.exception.http.ApiException;
import com.blikeng.job.executor.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
public class FileControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

    @Test
    void shouldReturn200WithFileIdOnUpload() throws Exception {
        String fileId = UUID.randomUUID() + "_test.txt";
        when(storageService.store(any())).thenReturn(fileId);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/file").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("File uploaded: " + fileId));
    }

    @Test
    void shouldReturn200WithFileContentOnDownload() throws Exception {
        byte[] content = "hello".getBytes();
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() { return "test.txt"; }
        };

        when(storageService.getFile(any())).thenReturn(resource);

        mockMvc.perform(get("/api/file/" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));
    }

    @Test
    void shouldReturn404ForUnknownFileId() throws Exception {
        when(storageService.getFile(any()))
                .thenThrow(new ApiException("File not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/file/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

package com.blikeng.job.executor.integrationTests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FileIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldUploadAndDownloadFile() throws Exception {
        byte[] fileContent = new ClassPathResource("files/my_file.txt").getContentAsByteArray();

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "my_file.txt", "text/plain", fileContent);

        String response = mockMvc.perform(multipart("/api/file").file(multipartFile))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String fileId = response.replace("File uploaded: ", "").trim();

        mockMvc.perform(get("/api/file/" + fileId))
                .andExpect(status().isOk())
                .andExpect(content().bytes(fileContent));
    }

    @Test
    void shouldReturnNotFoundForUnknownFileId() throws Exception {
        mockMvc.perform(get("/api/file/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
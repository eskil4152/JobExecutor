package com.blikeng.job.executor.integrationTests;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class JobIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateAndGetCompletedJob() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("a", 1);
        payload.put("b", 2);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("jobType", "ADD_NUMBERS");
        data.set("payload", payload);

        String jobId = mockMvc.perform(
                post("/api/job")
                        .header("Content-Type", "application/json")
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Thread.sleep(200);

        mockMvc.perform(
                get("/api/job/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobStatus").value("COMPLETED"));
    }

    @Test
    void shouldCreateAndGetFailedJobWhenWrongPayload() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("a", 1);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("jobType", "ADD_NUMBERS");
        data.set("payload", payload);

        String jobId = mockMvc.perform(
                        post("/api/job")
                                .header("Content-Type", "application/json")
                                .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Thread.sleep(200);

        mockMvc.perform(
                        get("/api/job/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobStatus").value("FAILED"))
                .andExpect(jsonPath("$.result").value("Invalid payload"));
    }

    @Test
    void shouldReturnNotFoundForUnknownJobId() throws Exception {
        mockMvc.perform(
                        get("/api/job/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestForMalformedRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("a", 1);
        payload.put("b", 2);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("jobType", "ADD_NUMBERS");

        mockMvc.perform(
                post("/api/job")
                        .header("Content-Type", "application/json")
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Job type and payload are required"));
    }

    @Test
    void shouldCreateAndGetCompletedHashJob() throws Exception {
        String input = "Hello, world!";
        String algorithm = "SHA-512";

        MessageDigest digest = MessageDigest.getInstance(algorithm);
        String expectedHash = HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));

        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("content", input);
        payload.put("algorithm", algorithm);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("jobType", "HASH_TEXT");
        data.set("payload", payload);

        String jobId = mockMvc.perform(
                        post("/api/job")
                                .header("Content-Type", "application/json")
                                .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Thread.sleep(200);

        mockMvc.perform(
                        get("/api/job/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.result.hash").value(expectedHash));
    }

    @Test
    void shouldHandleConcurrentJobSubmissions() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("a", 1);
        payload.put("b", 2);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("jobType", "ADD_NUMBERS");
        data.set("payload", payload);

        List<String> jobIds = new CopyOnWriteArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            pool.submit(() -> jobIds.add(
                    mockMvc.perform(
                            post("/api/job")
                                    .header("Content-Type", "application/json")
                                    .content(objectMapper.writeValueAsString(data)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString()
            ));
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        for (String id : jobIds) {
            Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() ->
                    mockMvc.perform(get("/api/job/" + id))
                            .andReturn().getResponse().getContentAsString()
                            .contains("COMPLETED")
            );
        }
    }
}
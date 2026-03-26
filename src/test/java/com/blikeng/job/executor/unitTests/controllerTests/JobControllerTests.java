package com.blikeng.job.executor.unitTests.controllerTests;

import com.blikeng.job.executor.controller.JobController;
import com.blikeng.job.executor.domain.JobStatus;
import com.blikeng.job.executor.domain.JobType;
import com.blikeng.job.executor.dto.JobDTO;
import com.blikeng.job.executor.dto.JobResponseDTO;
import com.blikeng.job.executor.exception.http.ApiException;
import com.blikeng.job.executor.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
public class JobControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturn200WithJobIdOnValidRequest() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(jobService.receiveTask(any())).thenReturn(jobId);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("a", 1);
        payload.put("b", 2);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("jobType", "ADD_NUMBERS");
        data.set("payload", payload);

        mockMvc.perform(post("/api/job")
                        .header("Content-Type", "application/json")
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(content().string(jobId.toString()));
    }

    @Test
    void shouldReturn200WithJobResponseOnValidId() throws Exception {
        String jobId = UUID.randomUUID().toString();

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("a", 1);
        payload.put("b", 2);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("sum", 3);

        JobResponseDTO response = new JobResponseDTO(
                jobId, JobType.ADD_NUMBERS, payload, result,
                JobStatus.COMPLETED, Instant.now(), Instant.now(), Instant.now()
        );

        when(jobService.getJob(jobId)).thenReturn(response);

        mockMvc.perform(get("/api/job/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.jobStatus").value("COMPLETED"));
    }

    @Test
    void shouldReturn404ForUnknownJobId() throws Exception {
        when(jobService.getJob(anyString()))
                .thenThrow(new ApiException("Job not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/job/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400ForMissingPayloadOrMissingJobType() throws Exception {
        when(jobService.receiveTask(any(JobDTO.class)))
                .thenThrow(new ApiException("Job type and payload are required", HttpStatus.BAD_REQUEST));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("jobType", "ADD_NUMBERS");

        mockMvc.perform(post("/api/job")
                        .header("Content-Type", "application/json")
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest());


        ObjectNode data2 = objectMapper.createObjectNode();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("a", 1);
        payload.put("b", 2);
        data2.set("payload", payload);

        mockMvc.perform(post("/api/job")
                        .header("Content-Type", "application/json")
                        .content(objectMapper.writeValueAsString(data2)))
                .andExpect(status().isBadRequest());
    }
}
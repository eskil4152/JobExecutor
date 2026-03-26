package com.blikeng.job.executor.dto;

import com.blikeng.job.executor.domain.JobStatus;
import com.blikeng.job.executor.domain.JobType;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record JobResponseDTO(
        String jobId,
        JobType jobType,
        JsonNode payload,
        JsonNode result,
        JobStatus jobStatus,
        Instant jobCreated,
        Instant jobStarted,
        Instant jobFinished
){}
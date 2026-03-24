package com.blikeng.job.executor.dto;

import com.blikeng.job.executor.domain.JobStatus;
import com.blikeng.job.executor.domain.JobType;

import java.time.Instant;

public class JobResponseDTO {
    private String jobId;
    private JobType jobType;
    private String payload;
    private String result;
    private JobStatus jobStatus;
    private Instant jobCreated;
    private Instant jobStarted;
    private Instant jobFinished;

    public JobResponseDTO(
            String jobId,
            JobType jobType,
            String payload,
            String result,
            JobStatus jobStatus,
            Instant jobCreated,
            Instant jobStarted,
            Instant jobFinished
    ) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.payload = payload;
        this.result = result;
        this.jobStatus = jobStatus;
        this.jobCreated = jobCreated;
        this.jobStarted = jobStarted;
        this.jobFinished = jobFinished;
    }

    public String getJobId() {
        return jobId;
    }

    public JobType getJobType() {
        return jobType;
    }

    public String getPayload() {
        return payload;
    }

    public String getResult() {
        return result;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public Instant getJobCreated() {
        return jobCreated;
    }

    public Instant getJobStarted() {
        return jobStarted;
    }

    public Instant getJobFinished() {
        return jobFinished;
    }
}
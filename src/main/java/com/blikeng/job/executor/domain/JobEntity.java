package com.blikeng.job.executor.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class JobEntity {
    @Id
    private UUID id = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    private String payload;

    private String result;

    @Enumerated(EnumType.STRING)
    private JobStatus jobStatus;

    private Instant jobCreated;
    private Instant jobStarted;
    private Instant jobCompleted;

    protected JobEntity() {}

    public JobEntity(JobType jobType, String payload) {
        this.jobType = jobType;
        this.payload = payload;
        this.jobStatus = JobStatus.QUEUED;
        this.jobCreated = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public JobType getJobType() {
        return jobType;
    }

    public String getPayload() {
        return payload;
    }

    public void markJobStarted() {
        this.jobStatus = JobStatus.RUNNING;
        this.jobStarted = Instant.now();
    }

    public void markJobFinished(String result) {
        this.jobStatus = JobStatus.COMPLETED;

        this.jobCompleted = Instant.now();
        if (result != null) {
            this.result = result;
        }
    }
}
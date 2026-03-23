package com.blikeng.job.executor.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class JobEntity {
    @Id
    private final UUID id = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private JobType jobType;

    @Column(name = "payload")
    private String payload;

    @Column(name = "result")
    private String result;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JobStatus jobStatus;

    @Column(name = "created")
    private Instant jobCreated;

    @Column(name = "started")
    private Instant jobStarted;

    @Column(name = "finished")
    private Instant jobFinished;

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

        this.jobFinished = Instant.now();
        this.result = result;
    }

    public void markJobFailed(String error) {
        this.jobStatus = JobStatus.FAILED;
        this.result = error;
    }
}
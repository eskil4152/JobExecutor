package com.blikeng.job.executor.worker;

import com.blikeng.job.executor.service.JobExecutionService;

import java.util.UUID;

public class JobTask implements Runnable {
    private final UUID jobId;
    private final JobExecutionService jobExecutionService;

    public JobTask(UUID jobId, JobExecutionService jobExecutionService) {
        this.jobId = jobId;
        this.jobExecutionService = jobExecutionService;
    }

    @Override
    public void run(){
        jobExecutionService.execute(jobId);
    }
}

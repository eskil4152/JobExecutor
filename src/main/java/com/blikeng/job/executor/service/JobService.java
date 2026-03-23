package com.blikeng.job.executor.service;

import com.blikeng.job.executor.domain.JobEntity;
import com.blikeng.job.executor.dto.JobDTO;
import com.blikeng.job.executor.repository.JobRepository;
import com.blikeng.job.executor.worker.JobTask;
import com.blikeng.job.executor.worker.WorkerManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JobService {
    private final WorkerManager workerManager;
    private final JobRepository jobRepository;
    private final JobExecutionService jobExecutionService;

    public JobService(
            WorkerManager workerManager,
            JobRepository jobRepository,
            JobExecutionService jobExecutionService
    ) {
        this.workerManager = workerManager;
        this.jobRepository = jobRepository;
        this.jobExecutionService = jobExecutionService;
    }

    public UUID receiveTask(JobDTO jobDTO){
        JobEntity job = new JobEntity(jobDTO.jobType(), jobDTO.payload());
        //jobRepository.save(job);

        workerManager.submitTask(new JobTask(job.getId(), jobExecutionService));

        return job.getId();
    }
}

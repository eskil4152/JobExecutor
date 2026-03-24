package com.blikeng.job.executor.service;

import com.blikeng.job.executor.domain.JobEntity;
import com.blikeng.job.executor.dto.JobDTO;
import com.blikeng.job.executor.dto.JobResponseDTO;
import com.blikeng.job.executor.repository.JobRepository;
import com.blikeng.job.executor.worker.JobTask;
import com.blikeng.job.executor.worker.WorkerManager;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JobService {
    private final WorkerManager workerManager;
    private final JobRepository jobRepository;
    private final JobExecutionService jobExecutionService;
    private final ObjectMapper objectMapper;

    public JobService(
            WorkerManager workerManager,
            JobRepository jobRepository,
            JobExecutionService jobExecutionService,
            ObjectMapper objectMapper
    ) {
        this.workerManager = workerManager;
        this.jobRepository = jobRepository;
        this.jobExecutionService = jobExecutionService;
        this.objectMapper = objectMapper;
    }

    public UUID receiveTask(JobDTO jobDTO){
        JobEntity job = new JobEntity(
                jobDTO.jobType(),
                objectMapper.writeValueAsString(jobDTO.payload()));

        jobRepository.save(job);

        workerManager.submitTask(new JobTask(job.getId(), jobExecutionService));

        return job.getId();
    }

    public JobResponseDTO getJob(String id){
        UUID uuid;

        try {
            uuid = UUID.fromString(id);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid job id");
        }

        JobEntity job = jobRepository.findById(uuid).orElse(null);

        if (job == null) {
            throw new IllegalArgumentException("Job not found");
        }

        return new JobResponseDTO(
                id,
                job.getJobType(),
                job.getPayload(),
                job.getResult(),
                job.getJobStatus(),
                job.getJobCreated(),
                job.getJobStarted(),
                job.getJobFinished()
        );
    }
}

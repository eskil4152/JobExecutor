package com.blikeng.job.executor.service;

import com.blikeng.job.executor.domain.JobEntity;
import com.blikeng.job.executor.dto.JobDTO;
import com.blikeng.job.executor.dto.JobResponseDTO;
import com.blikeng.job.executor.exception.Http.ApiException;
import com.blikeng.job.executor.repository.JobRepository;
import com.blikeng.job.executor.worker.JobTask;
import com.blikeng.job.executor.worker.WorkerManager;
import org.springframework.http.HttpStatus;
import tools.jackson.core.JacksonException;
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
        try {
            JobEntity job = new JobEntity(
                    jobDTO.jobType(),
                    objectMapper.writeValueAsString(jobDTO.payload())
            );

            jobRepository.save(job);
            workerManager.submitTask(new JobTask(job.getId(), jobExecutionService));

            return job.getId();
        } catch (JacksonException e) {
            throw new ApiException("Failed to create job from request payload", HttpStatus.BAD_REQUEST);
        }
    }

    public JobResponseDTO getJob(String id){
        UUID uuid;

        try {
            uuid = UUID.fromString(id);
        } catch (Exception e) {
            throw new ApiException("Passed ID was not valid UUID", HttpStatus.BAD_REQUEST);
        }

        JobEntity job = jobRepository.findById(uuid)
                .orElseThrow(() -> new ApiException("Requested job was not found", HttpStatus.NOT_FOUND));

        try {
            String payloadTemp = job.getPayload();
            String resultTemp = job.getResult();

            System.out.println("PAYLOAD: " + payloadTemp);
            System.out.println("RESULT: " + resultTemp);

            return new JobResponseDTO(
                    id,
                    job.getJobType(),
                    payloadTemp != null ? objectMapper.readTree(payloadTemp) : null,
                    resultTemp != null ? objectMapper.readTree(resultTemp) : null,
                    job.getJobStatus(),
                    job.getJobCreated(),
                    job.getJobStarted(),
                    job.getJobFinished()
            );
        } catch (JacksonException e) {
            throw new ApiException("Failed to read stored job data", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

package com.blikeng.job.executor.service;

import com.blikeng.job.executor.domain.JobEntity;
import com.blikeng.job.executor.dto.JobDTO;
import com.blikeng.job.executor.dto.JobResponseDTO;
import com.blikeng.job.executor.exception.http.ApiException;
import com.blikeng.job.executor.exception.messages.ApiMessages;
import com.blikeng.job.executor.repository.JobRepository;
import com.blikeng.job.executor.worker.JobTask;
import com.blikeng.job.executor.worker.WorkerManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

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
        if (jobDTO.jobType() == null || jobDTO.payload() == null) {
            throw new ApiException(ApiMessages.JOB_TYPE_AND_PAYLOAD_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }

        try {
            JobEntity job = new JobEntity(
                    jobDTO.jobType(),
                    objectMapper.writeValueAsString(jobDTO.payload())
            );

            jobRepository.save(job);
            workerManager.submitTask(new JobTask(job.getId(), jobExecutionService));

            return job.getId();
        } catch (JacksonException _) {
            throw new ApiException(ApiMessages.JOB_CREATION_FAILED.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public JobResponseDTO getJob(String id){
        try {
            UUID uuid = UUID.fromString(id);

            JobEntity job = jobRepository.findById(uuid)
                    .orElseThrow(() -> new ApiException(ApiMessages.JOB_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

            String payloadTemp = job.getPayload();
            String resultTemp = job.getResult();

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
        } catch (JacksonException _) {
            throw new ApiException(ApiMessages.JOB_READ_FAILED.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException _) {
            throw new ApiException(ApiMessages.INVALID_UUID.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}

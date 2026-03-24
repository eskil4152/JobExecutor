package com.blikeng.job.executor.service;

import com.blikeng.job.executor.domain.JobEntity;
import com.blikeng.job.executor.handler.JobHandler;
import com.blikeng.job.executor.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
public class JobExecutionService {
    private final JobRepository jobRepository;
    private final JobHandler jobHandler;

    private final Logger logger = LoggerFactory.getLogger(JobExecutionService.class);
    private final ObjectMapper objectMapper;

    public JobExecutionService(JobRepository jobRepository, JobHandler jobHandler, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.jobHandler = jobHandler;
        this.objectMapper = objectMapper;
    }

    public void execute(UUID jobId) {
        sleep();

        JobEntity job = jobRepository.findById(jobId)
            .orElseThrow(() -> {
                logger.error("Job {} not found", jobId);
                return new IllegalArgumentException("Job not found");
            });

        logger.info("Executing job {} of type {}", jobId, job.getJobType());

        job.markJobStarted();
        jobRepository.save(job);

        try {
            JsonNode result;

            switch (job.getJobType()) {
                case ADD_NUMBERS -> result = jobHandler.handleAddNumbers(job.getPayload());
                case COUNT_WORDS -> result = jobHandler.handleCountWords(job.getPayload());
                case ANALYZE_FILE -> result = jobHandler.handleFileAnalysis(job.getPayload());
                default -> {
                    logger.error("Job type {} not supported", job.getJobType());
                    throw new IllegalArgumentException("Job type not supported");
                }
            }

            String resultString = objectMapper.writeValueAsString(result);

            job.markJobFinished(resultString);
            jobRepository.save(job);

        } catch (Exception e) {
            job.markJobFailed("Job failed, please try again later.");
            jobRepository.save(job);

            logger.error("Job {} of type {} failed", jobId, job.getJobType(), e);
        }

        logger.info("Finished job {} of type {}", jobId, job.getJobType());
    }

    private void sleep() {
        try {
            Thread.sleep((long) 5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

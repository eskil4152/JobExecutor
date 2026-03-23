package com.blikeng.job.executor.service;

import com.blikeng.job.executor.domain.JobEntity;
import com.blikeng.job.executor.payloads.AddNumbersPayload;
import com.blikeng.job.executor.payloads.CountWordsPayload;
import com.blikeng.job.executor.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
public class JobExecutionService {
    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(JobExecutionService.class);

    public JobExecutionService(JobRepository jobRepository, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
    }

    public void execute(UUID jobId) {
        sleep(5000);

        JobEntity job = jobRepository.findById(jobId)
            .orElseThrow(() -> {
                logger.error("Job {} not found", jobId);
                return new IllegalArgumentException("Job not found");
            });

        logger.info("Executing job {} of type {}", jobId, job.getJobType());

        job.markJobStarted();
        jobRepository.save(job);

        try {
            String result;

            switch (job.getJobType()) {
                case ADD_NUMBERS -> result = handleAddNumbers(job);
                case COUNT_WORDS -> result = handleCountWords(job);
                default -> {
                    logger.error("Job type {} not supported", job.getJobType());
                    throw new IllegalArgumentException("Job type not supported");
                }
            }

            job.markJobFinished(result);
            jobRepository.save(job);

        } catch (Exception e) {
            job.markJobFailed("Job failed, please try again later.");
            jobRepository.save(job);

            logger.error("Job {} of type {} failed", jobId, job.getJobType(), e);
        }

        logger.info("Finished job {} of type {}", jobId, job.getJobType());
    }

    private String handleAddNumbers(JobEntity job) {
        AddNumbersPayload payload = objectMapper.readValue(job.getPayload(), AddNumbersPayload.class);

        return String.valueOf(payload.a() + payload.b());
    }

    private String handleCountWords(JobEntity job) {
        CountWordsPayload payload = objectMapper.readValue(job.getPayload(), CountWordsPayload.class);

        return "OK";
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

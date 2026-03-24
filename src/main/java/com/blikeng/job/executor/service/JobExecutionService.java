package com.blikeng.job.executor.service;

import com.blikeng.job.executor.domain.JobEntity;
import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.exception.JobException;
import com.blikeng.job.executor.exception.MetadataException;
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
        JobEntity job = jobRepository.findById(jobId)
            .orElseThrow(() -> {
                logger.error("Job {} not found", jobId);
                return new JobException("Job not found", jobId.toString());
            });

        logger.info("Executing job {} of type {}", jobId, job.getJobType());

        job.markJobStarted();
        jobRepository.save(job);

        JsonNode result;

        try {
            switch (job.getJobType()) {
                case ADD_NUMBERS -> result = jobHandler.handleAddNumbers(job.getPayload());
                case COUNT_WORDS -> result = jobHandler.handleCountWords(job.getPayload());
                case ANALYZE_FILE -> result = jobHandler.handleFileAnalysis(job.getPayload());
                case EXTRACT_METADATA -> result = jobHandler.handleMetadataExtraction(job.getPayload());
                default -> {
                    logger.error("Job type {} not supported", job.getJobType());
                    throw new JobException("Job type not supported", null);
                }
            }

            String resultString = objectMapper.writeValueAsString(result);

            job.markJobFinished(resultString);
            jobRepository.save(job);

        } catch (MetadataException e) {
            job.markJobFailed("Failed to extract metadata");
            jobRepository.save(job);

            logger.error("Job {} metadata extraction failure at {}: {}", jobId, e.getLocation(), e.getMessage(), e);

        } catch (InvalidPayloadException e) {
            job.markJobFailed("Invalid payload");
            jobRepository.save(job);

            logger.error("Job {} invalid payload: {}", jobId, e.getMessage(), e);

        } catch (FileProcessingException e) {
            job.markJobFailed("Failed to process file");
            jobRepository.save(job);

            logger.error("Job {} file processing failure: {}", jobId, e.getMessage(), e);
        }

        logger.info("Finished job {} of type {}", jobId, job.getJobType());
    }
}

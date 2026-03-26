package com.blikeng.job.executor.service;

import com.blikeng.job.executor.domain.JobEntity;
import com.blikeng.job.executor.exception.*;
import com.blikeng.job.executor.handler.*;
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

    private final CompressionHandler compressionHandler;
    private final FileAnalysisHandler fileAnalysisHandler;
    private final HashHandler hashHandler;
    private final JobHandler jobHandler;
    private final MetadataHandler metadataHandler;
    private final EncryptionHandler encryptionHandler;

    private final Logger logger = LoggerFactory.getLogger(JobExecutionService.class);
    private final ObjectMapper objectMapper;

    public JobExecutionService(
            JobRepository jobRepository,
            CompressionHandler compressionHandler,
            FileAnalysisHandler fileAnalysisHandler,
            HashHandler hashHandler,
            JobHandler jobHandler,
            MetadataHandler metadataHandler,
            EncryptionHandler encryptionHandler,
            ObjectMapper objectMapper
    ) {
        this.jobRepository = jobRepository;
        this.compressionHandler = compressionHandler;
        this.fileAnalysisHandler = fileAnalysisHandler;
        this.hashHandler = hashHandler;
        this.jobHandler = jobHandler;
        this.metadataHandler = metadataHandler;
        this.encryptionHandler = encryptionHandler;
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

        JsonNode result = null;

        try {
            switch (job.getJobType()) {
                case ADD_NUMBERS -> result = jobHandler.handleAddNumbers(job.getPayload());
                case COUNT_WORDS -> result = jobHandler.handleCountWords(job.getPayload());

                case ANALYZE_FILE -> result = fileAnalysisHandler.handleFileAnalysis(job.getPayload());
                case EXTRACT_METADATA -> result = metadataHandler.handleMetadataExtraction(job.getPayload());

                case HASH_FILE -> result = hashHandler.handleFileHashing(job.getPayload());
                case HASH_TEXT -> result = hashHandler.handleTextHashing(job.getPayload());
                case COMPARE_HASHES -> result = hashHandler.handleHashComparison(job.getPayload());

                case COMPRESS_FILE -> result = compressionHandler.handleFileCompression(job.getPayload());
                case DECOMPRESS_FILE -> result = compressionHandler.handleFileDecompression(job.getPayload());
                case COMPRESS_TEXT -> result = compressionHandler.handleTextCompression(job.getPayload());
                case DECOMPRESS_TEXT -> result = compressionHandler.handleTextDecompression(job.getPayload());

                case ENCRYPT_FILE -> result = encryptionHandler.handleFileEncryption(job.getPayload());
                case DECRYPT_FILE -> result = encryptionHandler.handleFileDecryption(job.getPayload());
                case ENCRYPT_TEXT -> result = encryptionHandler.handleTextEncryption(job.getPayload());
                case DECRYPT_TEXT -> result = encryptionHandler.handleTextDecryption(job.getPayload());
            }

            String resultString = objectMapper.writeValueAsString(result);

            job.markJobFinished(resultString);
            jobRepository.save(job);

        } catch (MetadataException e) {
            job.markJobFailed(objectMapper.writeValueAsString("Failed to extract metadata"));
            jobRepository.save(job);

            logger.error("Job {} metadata extraction failure at {}: {}", jobId, e.getLocation(), e.getMessage(), e);

        } catch (InvalidPayloadException e) {
            job.markJobFailed(objectMapper.writeValueAsString("Invalid payload"));
            jobRepository.save(job);

            logger.error("Job {} invalid got payload at {}: {}", jobId, e.getLocation(), e.getMessage(), e);

        } catch (FileProcessingException e) {
            job.markJobFailed(objectMapper.writeValueAsString("Failed to process file"));
            jobRepository.save(job);

            logger.error("Job {} file processing failure at {}: {}", jobId, e.getLocation(), e.getMessage(), e);
        } catch (AlgorithmException e) {
            job.markJobFailed(objectMapper.writeValueAsString("Algorithm not found"));
            jobRepository.save(job);

            logger.error("Job {} failed at {} with algorithm: {}: {}", jobId, e.getAlgorithm(), e.getLocation(), e.getMessage(), e);
        }

        logger.info("Finished job {} of type {}", jobId, job.getJobType());
    }
}

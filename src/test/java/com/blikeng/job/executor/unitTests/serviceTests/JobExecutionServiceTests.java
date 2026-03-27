package com.blikeng.job.executor.unitTests.serviceTests;

import com.blikeng.job.executor.domain.JobEntity;
import com.blikeng.job.executor.domain.JobStatus;
import com.blikeng.job.executor.domain.JobType;
import com.blikeng.job.executor.exception.*;
import com.blikeng.job.executor.handler.*;
import com.blikeng.job.executor.repository.JobRepository;
import com.blikeng.job.executor.service.JobExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobExecutionServiceTests {
    // ==========================
    // Tests for JobExecutionService. Verifies:
    // - Job not found throws exception
    // - Status transitions: QUEUED → RUNNING → COMPLETED / FAILED
    // - Correct handler dispatched for every JobType
    // - Each exception type marks job as FAILED with correct error message
    // ==========================

    @Mock private JobRepository jobRepository;
    @Mock private JobHandler jobHandler;
    @Mock private HashHandler hashHandler;
    @Mock private CompressionHandler compressionHandler;
    @Mock private EncryptionHandler encryptionHandler;
    @Mock private MetadataHandler metadataHandler;
    @Mock private FileAnalysisHandler fileAnalysisHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JobExecutionService jobExecutionService;

    @BeforeEach
    void setUp() {
        jobExecutionService = new JobExecutionService(
                jobRepository, compressionHandler, fileAnalysisHandler,
                hashHandler, jobHandler, metadataHandler, encryptionHandler, objectMapper
        );
    }

    // ==========================
    // Job not found
    // ==========================
    @Test
    void shouldThrowJobExceptionWhenJobNotFound() {
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobExecutionService.execute(id))
                .isInstanceOf(JobException.class);
    }

    // ==========================
    // Status transitions
    // ==========================
    @Test
    void shouldMarkJobCompletedOnSuccessfulExecution() {
        JobEntity job = new JobEntity(JobType.ADD_NUMBERS, """
                {"a": 1, "b": 2}
                """);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("sum", 3);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobHandler.handleAddNumbers(any())).thenReturn(result);

        jobExecutionService.execute(job.getId());

        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getResult()).contains("3");
        verify(jobRepository, times(2)).save(job);
    }

    @Test
    void shouldMarkJobRunningBeforeExecution() {
        JobEntity job = new JobEntity(JobType.ADD_NUMBERS, """
                {"a": 1, "b": 2}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobHandler.handleAddNumbers(any())).thenReturn(emptyResult);

        List<JobStatus> savedStatuses = new ArrayList<>();
        doAnswer(invocation -> {
            savedStatuses.add(invocation.<JobEntity>getArgument(0).getJobStatus());
            return null;
        }).when(jobRepository).save(any());

        jobExecutionService.execute(job.getId());

        assertThat(savedStatuses).containsExactly(JobStatus.RUNNING, JobStatus.COMPLETED);
    }

    // ==========================
    // Handler dispatch
    // ==========================
    @Test
    void shouldDispatchToJobHandlerForAddNumbersJob() {
        JobEntity job = new JobEntity(JobType.ADD_NUMBERS, """
                {"a": 1, "b": 2}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobHandler.handleAddNumbers(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(jobHandler).handleAddNumbers(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToJobHandlerForCountWordsJob() {
        JobEntity job = new JobEntity(JobType.COUNT_WORDS, """
                {"content": "hello world"}
                """);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobHandler.handleCountWords(any())).thenReturn(objectMapper.createObjectNode());

        jobExecutionService.execute(job.getId());

        verify(jobHandler).handleCountWords(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToFileAnalysisHandlerForAnalyzeFileJob() {
        JobEntity job = new JobEntity(JobType.ANALYZE_FILE, """
                {"fileId": "hello.file"}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(fileAnalysisHandler.handleFileAnalysis(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(fileAnalysisHandler).handleFileAnalysis(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToMetadataHandlerForExtractMetadataJob() {
        JobEntity job = new JobEntity(JobType.EXTRACT_METADATA, """
                {"fileId": "file.file"}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(metadataHandler.handleMetadataExtraction(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(metadataHandler).handleMetadataExtraction(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToHashHandlerForHashFileJob() {
        JobEntity job = new JobEntity(JobType.HASH_FILE, """
                {"fileId": "hello.file"}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(hashHandler.handleFileHashing(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(hashHandler).handleFileHashing(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToHashHandlerForHashTextJob() {
        JobEntity job = new JobEntity(JobType.HASH_TEXT, """
                {"content": "hello", "algorithm": "SHA-256"}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(hashHandler.handleTextHashing(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(hashHandler).handleTextHashing(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToHashHandlerForCompareHashesJob() {
        JobEntity job = new JobEntity(JobType.COMPARE_HASHES, """
                {"hashA": "abc123", "hashB": "abc123"}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(hashHandler.handleHashComparison(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(hashHandler).handleHashComparison(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToCompressionHandlerForCompressFileJob() {
        JobEntity job = new JobEntity(JobType.COMPRESS_FILE, """
                {"fileId": "hello.file"}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(compressionHandler.handleFileCompression(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(compressionHandler).handleFileCompression(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToCompressionHandlerForDecompressFileJob() {
        JobEntity job = new JobEntity(JobType.DECOMPRESS_FILE, """
                {"fileId": "hello.file"}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(compressionHandler.handleFileDecompression(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(compressionHandler).handleFileDecompression(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToCompressionHandlerForCompressTextJob() {
        JobEntity job = new JobEntity(JobType.COMPRESS_TEXT, """
                {"content": "hello"}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(compressionHandler.handleTextCompression(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(compressionHandler).handleTextCompression(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToCompressionHandlerForDecompressTextJob() {
        JobEntity job = new JobEntity(JobType.DECOMPRESS_TEXT, """
                {"content": "hello"}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(compressionHandler.handleTextDecompression(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(compressionHandler).handleTextDecompression(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToEncryptionHandlerForEncryptFileJob() {
        JobEntity job = new JobEntity(JobType.ENCRYPT_FILE, """
                {"fileId": "hello.file"}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(encryptionHandler.handleFileEncryption(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(encryptionHandler).handleFileEncryption(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToEncryptionHandlerForDecryptFileJob() {
        JobEntity job = new JobEntity(JobType.DECRYPT_FILE, """
                {"fileId": "hello.file.enc"}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(encryptionHandler.handleFileDecryption(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(encryptionHandler).handleFileDecryption(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToEncryptionHandlerForEncryptTextJob() {
        JobEntity job = new JobEntity(JobType.ENCRYPT_TEXT, """
                {"content": "hello", "key": "abc"}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(encryptionHandler.handleTextEncryption(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(encryptionHandler).handleTextEncryption(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void shouldDispatchToEncryptionHandlerForDecryptTextJob() {
        JobEntity job = new JobEntity(JobType.DECRYPT_TEXT, """
                {"content": "hello"}
                """);
        ObjectNode emptyResult = objectMapper.createObjectNode();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(encryptionHandler.handleTextDecryption(any())).thenReturn(emptyResult);

        jobExecutionService.execute(job.getId());

        verify(encryptionHandler).handleTextDecryption(any());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    // ==========================
    // Failure paths
    // ==========================
    @Test
    void shouldMarkJobFailedOnInvalidPayloadException() {
        JobEntity job = new JobEntity(JobType.ADD_NUMBERS, """
                {"a": 1}
                """);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobHandler.handleAddNumbers(any()))
                .thenThrow(new InvalidPayloadException("Missing field b", "JobHandler", null));

        jobExecutionService.execute(job.getId());

        assertThat(job.getJobStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getResult()).contains("Invalid payload");
    }

    @Test
    void shouldMarkJobFailedOnMetadataException() {
        JobEntity job = new JobEntity(JobType.EXTRACT_METADATA, """
                {"fileId": "abc"}
                """);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(metadataHandler.handleMetadataExtraction(any()))
                .thenThrow(new MetadataException("Cannot read metadata", "MetadataHandler", null));

        jobExecutionService.execute(job.getId());

        assertThat(job.getJobStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getResult()).contains("Failed to extract metadata");
    }

    @Test
    void shouldMarkJobFailedOnFileProcessingException() {
        JobEntity job = new JobEntity(JobType.HASH_FILE, """
                {"fileId": "abc"}
                """);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(hashHandler.handleFileHashing(any()))
                .thenThrow(new FileProcessingException("Cannot read file", "HashHandler", null));

        jobExecutionService.execute(job.getId());

        assertThat(job.getJobStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getResult()).contains("Failed to process file");
    }

    @Test
    void shouldMarkJobFailedOnAlgorithmException() {
        JobEntity job = new JobEntity(JobType.HASH_TEXT, """
                {"content": "hello", "algorithm": "INVALID"}
                """);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(hashHandler.handleTextHashing(any()))
                .thenThrow(new AlgorithmException("Algorithm not found", "HashHandler", "INVALID", null));

        jobExecutionService.execute(job.getId());

        assertThat(job.getJobStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getResult()).contains("Algorithm not found");
    }
}

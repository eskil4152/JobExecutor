package com.blikeng.job.executor.unitTests.serviceTests;

import com.blikeng.job.executor.domain.JobEntity;
import com.blikeng.job.executor.domain.JobType;
import com.blikeng.job.executor.dto.JobDTO;
import com.blikeng.job.executor.exception.http.ApiException;
import com.blikeng.job.executor.repository.JobRepository;
import com.blikeng.job.executor.service.JobExecutionService;
import com.blikeng.job.executor.service.JobService;
import com.blikeng.job.executor.worker.WorkerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JobServiceTests {
    // ==========================
    // Tests for JobService. Verifies:
    // - Valid job submission saves entity and submits task
    // - Null jobType or payload throws 400
    // - Invalid UUID string on getJob throws 400
    // - Unknown job UUID on getJob throws 404
    // - Valid UUID returns mapped JobResponseDTO
    // ==========================

    @Mock private WorkerManager workerManager;
    @Mock private JobRepository jobRepository;
    @Mock private JobExecutionService jobExecutionService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(workerManager, jobRepository, jobExecutionService, objectMapper);
    }

    // ==========================
    // Submit Jobs
    // ==========================
    @Test
    void shouldSaveJobAndReturnIdOnValidRequest() {
        ArgumentCaptor<JobEntity> captor = ArgumentCaptor.forClass(JobEntity.class);
        JobDTO jobDTO = new JobDTO(JobType.ADD_NUMBERS, objectMapper.createObjectNode());

        when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UUID id = jobService.receiveTask(jobDTO);

        verify(jobRepository).save(captor.capture());

        JobEntity saved = captor.getValue();

        assertEquals(saved.getId(), id);
    }

    @Test
    void shouldThrowBadRequestWhenJobTypeIsNull() {
        JobDTO jobDTO = new JobDTO(null, objectMapper.createObjectNode());

        assertThatThrownBy(() -> jobService.receiveTask(jobDTO))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void shouldThrowBadRequestWhenPayloadIsNull() {
        JobDTO jobDTO = new JobDTO(JobType.ADD_NUMBERS, null);

        assertThatThrownBy(() -> jobService.receiveTask(jobDTO))
                .isInstanceOf(ApiException.class);
    }

    // ==========================
    // Get Jobs
    // ==========================
    @Test
    void shouldThrowBadRequestForInvalidUUID() {
        assertThatThrownBy(() -> jobService.getJob("not a UUID")).isInstanceOf(ApiException.class);
    }

    @Test
    void shouldThrowNotFoundForUnknownJobId() {
        when(jobRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJob(UUID.randomUUID().toString())).isInstanceOf(ApiException.class);
    }

    @Test
    void shouldThrowInternalServerErrorWhenStoredPayloadIsInvalidJson() {
        UUID id = UUID.randomUUID();

        JobEntity job = org.mockito.Mockito.mock(JobEntity.class);
        when(job.getPayload()).thenReturn("{invalid json");
        when(job.getResult()).thenReturn(null);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.getJob(id.toString()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void shouldThrowInternalServerErrorWhenStoredResultIsInvalidJson() {
        UUID id = UUID.randomUUID();

        JobEntity job = org.mockito.Mockito.mock(JobEntity.class);
        when(job.getPayload()).thenReturn("{}");
        when(job.getResult()).thenReturn("{invalid json");

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.getJob(id.toString()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void shouldReturnJobResponseDTOWhenPayloadAndResultAreNull() {
        UUID id = UUID.randomUUID();

        JobEntity job = org.mockito.Mockito.mock(JobEntity.class);
        when(job.getPayload()).thenReturn(null);
        when(job.getResult()).thenReturn(null);
        when(job.getJobType()).thenReturn(JobType.ADD_NUMBERS);
        when(job.getJobStatus()).thenReturn(null);
        when(job.getJobCreated()).thenReturn(null);
        when(job.getJobStarted()).thenReturn(null);
        when(job.getJobFinished()).thenReturn(null);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        var response = jobService.getJob(id.toString());

        assertEquals(id.toString(), response.getJobId());
        assertEquals(JobType.ADD_NUMBERS, response.getJobType());
        assertNull(response.getPayload());
        assertNull(response.getResult());
    }

    @Test
    void shouldReturnJobResponseDTOWhenPayloadIsNullAndResultIsPresent() {
        UUID id = UUID.randomUUID();

        JobEntity job = org.mockito.Mockito.mock(JobEntity.class);
        when(job.getPayload()).thenReturn(null);
        when(job.getResult()).thenReturn("{\"sum\":3}");
        when(job.getJobType()).thenReturn(JobType.ADD_NUMBERS);
        when(job.getJobStatus()).thenReturn(null);
        when(job.getJobCreated()).thenReturn(null);
        when(job.getJobStarted()).thenReturn(null);
        when(job.getJobFinished()).thenReturn(null);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        var response = jobService.getJob(id.toString());

        assertEquals(null, response.getPayload());
        assertEquals(3, response.getResult().get("sum").asInt());
    }

    @Test
    void shouldReturnJobResponseDTOWhenPayloadIsPresentAndResultIsNull() {
        UUID id = UUID.randomUUID();

        JobEntity job = org.mockito.Mockito.mock(JobEntity.class);
        when(job.getPayload()).thenReturn("{\"a\":1}");
        when(job.getResult()).thenReturn(null);
        when(job.getJobType()).thenReturn(JobType.ADD_NUMBERS);
        when(job.getJobStatus()).thenReturn(null);
        when(job.getJobCreated()).thenReturn(null);
        when(job.getJobStarted()).thenReturn(null);
        when(job.getJobFinished()).thenReturn(null);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        var response = jobService.getJob(id.toString());

        assertEquals(1, response.getPayload().get("a").asInt());
        assertEquals(null, response.getResult());
    }
}

package com.blikeng.job.executor.service;

import com.blikeng.job.executor.domain.JobEntity;
import com.blikeng.job.executor.payloads.AddNumbersPayload;
import com.blikeng.job.executor.payloads.CountWordsPayload;
import com.blikeng.job.executor.repository.JobRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
public class JobExecutionService {
    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    public JobExecutionService(JobRepository jobRepository, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
    }

    public void execute(UUID jobId) {
        System.out.println("Started");

        sleep(5000);

        JobEntity job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        job.markJobStarted();
        jobRepository.save(job);

        try {
            String result;

            switch (job.getJobType()) {
                case ADD_NUMBERS -> result = handleAddNumbers(job);
                case COUNT_WORDS -> result = handleCountWords(job);
                default -> throw new IllegalArgumentException("Unsupported job type");
            }

            job.markJobFinished(result);
            jobRepository.save(job);

        } catch (Exception e) {
            job.markJobFailed(e.getMessage());
            jobRepository.save(job);
        }

        System.out.println("Finished");
    }

    private String handleAddNumbers(JobEntity job) {
        AddNumbersPayload payload = objectMapper.readValue(job.getPayload(), AddNumbersPayload.class);

        sleep(5001);

        return String.valueOf(payload.a() + payload.b());
    }

    private String handleCountWords(JobEntity job) {
        CountWordsPayload payload = objectMapper.readValue(job.getPayload(), CountWordsPayload.class);

        sleep(5002);

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

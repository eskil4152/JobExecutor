package com.blikeng.job.executor.service;

import com.blikeng.job.executor.domain.JobEntity;
import com.blikeng.job.executor.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JobExecutionService {
    private final JobRepository jobRepository;

    public JobExecutionService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public void execute(UUID jobId) {
        System.out.println("Started");

        sleep(5000);


        /*JobEntity job = jobRepository.findById(jobId)
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
            job.markJobFinished(e.getMessage());
            jobRepository.save(job);
        }*/

        System.out.println("Finished");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String handleAddNumbers(JobEntity job) {
        String payload = job.getPayload();
        sleep(5001);

        return "OK";
    }

    private String handleCountWords(JobEntity job) {
        String payload = job.getPayload();
        sleep(5000);

        return "OK";
    }
}

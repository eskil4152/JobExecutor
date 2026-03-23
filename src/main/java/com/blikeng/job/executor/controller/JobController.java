package com.blikeng.job.executor.controller;

import com.blikeng.job.executor.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/job")
    public ResponseEntity<String> createJob() {
        return ResponseEntity.ok().body("Job has been created with id " + "123");
    }

    @GetMapping("/job/{id}")
    public ResponseEntity<String> getJob(
            @PathVariable String id
    ){
        jobService.doSomething();

        return ResponseEntity.ok().body("Got job with id " + id);
    }
}

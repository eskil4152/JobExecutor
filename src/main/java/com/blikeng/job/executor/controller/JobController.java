package com.blikeng.job.executor.controller;

import com.blikeng.job.executor.dto.JobDTO;
import com.blikeng.job.executor.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/job")
    public ResponseEntity<String> createJob(
            @RequestBody JobDTO jobDTO
            ) {

        UUID id = jobService.receiveTask(jobDTO);

        return ResponseEntity.ok().body(id.toString());
    }

    @GetMapping("/job/{id}")
    public ResponseEntity<String> getJob(
            @PathVariable String id
    ){
        return ResponseEntity.ok().body("Got job with id " + id);
    }
}

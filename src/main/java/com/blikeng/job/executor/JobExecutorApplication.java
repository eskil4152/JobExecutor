package com.blikeng.job.executor;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class JobExecutorApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(JobExecutorApplication.class).run(args);
    }
}
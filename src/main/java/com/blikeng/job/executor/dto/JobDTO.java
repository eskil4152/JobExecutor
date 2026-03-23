package com.blikeng.job.executor.dto;

import com.blikeng.job.executor.domain.JobType;

public record JobDTO (
   JobType jobType,
   String payload
){}

package com.blikeng.job.executor.dto;

import com.blikeng.job.executor.domain.JobType;
import tools.jackson.databind.JsonNode;

public record JobDTO (
   JobType jobType,
   JsonNode payload
){}


package com.blikeng.job.executor.repository;

import com.blikeng.job.executor.domain.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, UUID> {
}

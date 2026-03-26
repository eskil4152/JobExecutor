package com.blikeng.job.executor.unitTests.worker;

import com.blikeng.job.executor.worker.WorkerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkerManagerTests {
    @Mock
    ExecutorService executorService;
    WorkerManager workerManager;

    @BeforeEach
    void setUp() { workerManager = new WorkerManager(executorService); }

    @Test
    void shouldSubmitTaskToExecutor() {
        Runnable task = () -> {};
        workerManager.submitTask(task);
        verify(executorService).submit(task);
    }

    @Test
    void shouldShutdownExecutorOnStop() {
        workerManager.stopWorkers();
        verify(executorService).shutdown();
    }
}
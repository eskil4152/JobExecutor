package com.blikeng.job.executor.worker;

import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

@Service
public class WorkerManager {
    private final ExecutorService executorService;

    public WorkerManager(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void submitTask(Runnable task){
        executorService.submit(task);
    }

    public void stopWorkers(){
        executorService.shutdown();
    }
}

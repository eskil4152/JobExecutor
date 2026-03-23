package com.blikeng.job.executor.service;

import com.blikeng.job.executor.worker.WorkerManager;
import com.blikeng.job.executor.worker.MathTask;
import com.blikeng.job.executor.worker.StringTask;
import org.springframework.stereotype.Service;

@Service
public class JobService {
    private final WorkerManager workerManager;

    public JobService(WorkerManager workerManager) {
        this.workerManager = workerManager;
    }

    public void doSomething(){
        MathTask mathTask = new MathTask();
        StringTask stringTask = new StringTask();

        workerManager.submitTask(mathTask);
        workerManager.submitTask(stringTask);

        workerManager.stopWorkers();
    }
}

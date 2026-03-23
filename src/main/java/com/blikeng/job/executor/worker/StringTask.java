package com.blikeng.job.executor.worker;

public class StringTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Pooled task is running...");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("String task has completed.");
    }
}

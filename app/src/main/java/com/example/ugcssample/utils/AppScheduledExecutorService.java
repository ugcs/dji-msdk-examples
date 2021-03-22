package com.example.ugcssample.utils;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AppScheduledExecutorService {


    private final String executorServiceName;
    private final ScheduledExecutorService worker;

    AppScheduledExecutorService(String executorServiceName, ScheduledExecutorService worker) {
        this.executorServiceName = executorServiceName;
        this.worker = worker;
    }

    public Future<?> submit(Runnable task) {
        return worker.submit(task);
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return worker.schedule(command, delay, unit);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return worker.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return worker.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    public String getExecutorServiceName() {
        return executorServiceName;
    }
}

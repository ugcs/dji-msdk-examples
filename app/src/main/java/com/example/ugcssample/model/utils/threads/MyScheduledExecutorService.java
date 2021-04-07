package com.example.ugcssample.model.utils.threads;


import com.example.ugcssample.model.utils.AppUtils;
import com.example.ugcssample.model.utils.Logger;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MyScheduledExecutorService {

    private final String executorServiceName;
    private final Logger logger;
    private final ScheduledExecutorService worker;

    MyScheduledExecutorService(String executorServiceName, Logger logger, ScheduledExecutorService worker) {
        this.executorServiceName = executorServiceName;
        this.logger = logger;
        this.worker = worker;
    }

    public Future<?> submit(Runnable task) {
        if (AppUtils.debug) {
            return worker.submit(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    AppUtils.crashApplication(MyScheduledExecutorService.this, e);
                }
            });
        } else {
            return worker.submit(task);
        }
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        if (AppUtils.debug) {
            return worker.schedule(() -> {
                try {
                    command.run();
                } catch (Exception e) {
                    AppUtils.crashApplication(MyScheduledExecutorService.this, e);
                }
            }, delay, unit);
        } else {
            return worker.schedule(command, delay, unit);
        }
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (AppUtils.debug) {
            return worker.scheduleAtFixedRate(() -> {
                try {
                    command.run();
                } catch (Exception e) {
                    AppUtils.crashApplication(MyScheduledExecutorService.this, e);
                }
            }, initialDelay, period, unit);
        } else {
            return worker.scheduleAtFixedRate(command, initialDelay, period, unit);
        }
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (AppUtils.debug) {
            return worker.scheduleWithFixedDelay(() -> {
                try {
                    command.run();
                } catch (Exception e) {
                    AppUtils.crashApplication(MyScheduledExecutorService.this, e);
                }
            }, initialDelay, delay, unit);
        } else {
            return worker.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public String getExecutorServiceName() {
        return executorServiceName;
    }
}

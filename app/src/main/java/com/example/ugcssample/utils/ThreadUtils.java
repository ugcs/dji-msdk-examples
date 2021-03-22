package com.example.ugcssample.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public final class ThreadUtils {

    private ThreadUtils() {
        // Utility class.
    }

    public static AppScheduledExecutorService newSingleThreadScheduledExecutor(final Class clazz) {
        String executorServiceName = "worker-" + clazz.getSimpleName();
        ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, executorServiceName);
            }
        });
        return new AppScheduledExecutorService(executorServiceName, worker);
    }
}
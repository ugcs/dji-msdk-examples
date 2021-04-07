package com.example.ugcssample.model.utils.threads;


import com.example.ugcssample.model.utils.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public final class ThreadUtils {

    private ThreadUtils() {
        // Utility class.
    }

    public static MyScheduledExecutorService newSingleThreadScheduledExecutor(Logger logger, final Class clazz) {
        String executorServiceName = "worker-" + clazz.getSimpleName();
        ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, executorServiceName);
            }
        });
        return new MyScheduledExecutorService(executorServiceName, logger, worker);
    }
    
}

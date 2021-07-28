package com.example.ugcssample.utils

import java.util.concurrent.Executors

object ThreadUtils {
    fun newSingleThreadScheduledExecutor(clazz: Class<*>?): AppScheduledExecutorService {
        val executorServiceName = "worker-" + clazz?.simpleName
        val worker =
            Executors.newSingleThreadScheduledExecutor { r -> Thread(r, executorServiceName) }
        return AppScheduledExecutorService(executorServiceName, worker)
    }
}
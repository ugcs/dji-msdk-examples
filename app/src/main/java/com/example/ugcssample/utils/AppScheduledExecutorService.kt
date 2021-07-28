package com.example.ugcssample.utils

import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class AppScheduledExecutorService internal constructor(
    private val executorServiceName: String?,
    private val worker: ScheduledExecutorService
                                                      ) {
    fun submit(task: Runnable?): Future<*>? {
        return worker.submit(task)
    }

    fun schedule(command: Runnable?, delay: Long, unit: TimeUnit?): ScheduledFuture<*>? {
        return worker.schedule(command, delay, unit)
    }

    fun scheduleAtFixedRate(
        command: Runnable?,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit?
                           ): ScheduledFuture<*>? {
        return worker.scheduleAtFixedRate(command, initialDelay, period, unit)
    }

    fun scheduleWithFixedDelay(
        command: Runnable?,
        initialDelay: Long,
        delay: Long,
        unit: TimeUnit?
                              ): ScheduledFuture<*>? {
        return worker.scheduleWithFixedDelay(command, initialDelay, delay, unit)
    }

    fun getExecutorServiceName(): String? {
        return executorServiceName
    }
}
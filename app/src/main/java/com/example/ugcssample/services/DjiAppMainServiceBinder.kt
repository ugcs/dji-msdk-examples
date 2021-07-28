package com.example.ugcssample.services

import android.os.Binder

class DjiAppMainServiceBinder(private val service: DjiAppMainService?) : Binder() {
    fun getService(): DjiAppMainService? {
        return service
    }
}
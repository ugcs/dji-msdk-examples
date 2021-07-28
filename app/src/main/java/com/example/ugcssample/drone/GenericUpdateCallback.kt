package com.example.ugcssample.drone

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.ugcssample.utils.ThreadUtils

abstract class GenericUpdateCallback(private val lbm: LocalBroadcastManager?) {
    abstract fun setUpKeyListeners()
    abstract fun tearDownKeyListeners()
    protected fun sendBroadcast(i: Intent?) {
        if (i != null) {
            lbm?.sendBroadcast(i)
        }
    }

    protected fun submitBroadcast(i: Intent?) {
        WORKER.submit { sendBroadcast(i) }
    }

    companion object {
        protected val WORKER = ThreadUtils.newSingleThreadScheduledExecutor(
            GenericUpdateCallback::class.java
                                                                           )
    }
}
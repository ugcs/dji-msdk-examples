package com.example.ugcssample.drone;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.ugcssample.utils.AppScheduledExecutorService;
import com.example.ugcssample.utils.ThreadUtils;

public abstract class GenericUpdateCallback {

    protected static final AppScheduledExecutorService WORKER
            = ThreadUtils.newSingleThreadScheduledExecutor(GenericUpdateCallback.class);

    private final LocalBroadcastManager lbm;

    public GenericUpdateCallback(LocalBroadcastManager lbm) {
        this.lbm = lbm;
    }

    public abstract void setUpKeyListeners();

    public abstract void tearDownKeyListeners();

    protected void sendBroadcast(Intent i) {
        lbm.sendBroadcast(i);
    }

    protected void submitBroadcast(final Intent i) {
        WORKER.submit(new Runnable() {
            @Override
            public void run() {
                sendBroadcast(i);
            }
        });
    }

}
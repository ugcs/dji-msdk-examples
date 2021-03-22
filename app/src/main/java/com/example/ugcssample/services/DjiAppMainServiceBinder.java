package com.example.ugcssample.services;

import android.os.Binder;
public class DjiAppMainServiceBinder extends Binder {

    private final DjiAppMainService service;

    public DjiAppMainServiceBinder(DjiAppMainService service) {
        this.service = service;
    }

    public DjiAppMainService getService() {
        return service;
    }

}

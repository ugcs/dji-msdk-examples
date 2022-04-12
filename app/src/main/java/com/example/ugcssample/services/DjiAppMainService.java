package com.example.ugcssample.services;

import android.os.Handler;

public interface DjiAppMainService {
    void init();
    void startSimulator();
    void takeCapture(Handler handler, String xmpTag);
}

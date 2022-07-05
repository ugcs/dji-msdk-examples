package com.example.ugcssample.services;

import java.io.File;

public interface DjiAppMainService {
    void init();
    void startSimulator();
    void uploadMission();
    void startMission();
    
    void uploadMission(File nativeRoute);
    
    void takeOff();
    void land(boolean useKeyInterface);
    void cancelMission();

    void missionExecutionStatusChanged();
}

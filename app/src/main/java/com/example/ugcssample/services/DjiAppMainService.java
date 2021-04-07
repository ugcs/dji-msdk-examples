package com.example.ugcssample.services;

public interface DjiAppMainService {
    void init();
    void startSimulator();
    void uploadAscendMission();
    void startMission();
    Double getLatitude();
    Double getLongitude();
    Float getAltitude();
}

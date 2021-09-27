package com.example.ugcssample.services;

import com.example.ugcssample.drone.BeaconController;

public interface DjiAppMainService {
    void init();
    void startSimulator();
    BeaconController getBeaconController();
}

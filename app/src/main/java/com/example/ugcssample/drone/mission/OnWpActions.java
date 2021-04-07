package com.example.ugcssample.drone.mission;

import com.example.ugcssample.model.command.CameraAttitude;
import com.example.ugcssample.model.command.CameraMediaFileInfo;
import com.example.ugcssample.model.command.CameraSeriesTime;
import com.example.ugcssample.model.command.CameraZoom;
import com.example.ugcssample.model.command.ChangeSpeed;
import com.example.ugcssample.model.command.MissionPause;

public class OnWpActions {
    public ChangeSpeed changeSpeed = null;
    public CameraSeriesTime seriesByTime = null;
    public CameraAttitude cameraAttitude = null;
    public CameraZoom cameraZoom = null;
    public CameraMediaFileInfo cameraMediaFileInfo = null;
    public MissionPause missionPause = null;

}

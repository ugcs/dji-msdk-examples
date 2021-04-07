package com.example.ugcssample.model;

/**
 * this field will not be stored in a file system.
 * use it as a temp parameter
 */
public class MissionTransientAttributes {
    public String nativeHeadingMode = null;
    //public boolean globalPoi = false;
    public boolean forceReturnAltitudeChange = false;
    public String transientTag;
    public String transientDescription;
    public boolean recoverWaypointDistanceTooClose = true;
    public boolean forceOnWpActionForCameraAttitude = false;

}

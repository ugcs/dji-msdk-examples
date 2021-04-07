package com.example.ugcssample.drone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import dji.common.error.DJIError;
import dji.common.mission.waypoint.WaypointExecutionProgress;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecuteState;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import timber.log.Timber;

public class MyMissionGlobalEventListener implements WaypointMissionOperatorListener, CommonCallbacks
        .CompletionCallback {

    public interface MainController {

        void onMissionUploadResult(DJIError djiError);

        void onMissionUploadUpdate(WaypointMissionUploadEvent waypointMissionUploadEvent);

        void onMissionExecutionStart();

        void onMissionWpReached(int reachedWpIndex);

        void onMissionWpMovingTo(int movingToWpIndex);

        void onMissionExecutionFinish();
    }

    private final MainController droneMainController;

    private int lastKnownReacedPoint = -1;
    private int lastKnownMovingToPoint = -1;

    private Integer djiTargetPoint = null;
    private Boolean djiIsWaypointReached = null;
    private WaypointMissionExecuteState djiExecState = null;
    private Integer djiTotalPointCnt = null;
    private long lastMissionStartedTime = 0;
    private long lastExecuteStateUpdateTime = 0;

    public MyMissionGlobalEventListener(MainController droneMainController) {
        this.droneMainController = droneMainController;
    }

    @Override
    public void onResult(DJIError djiError) {
        if (djiError == null) {
            // !!! this method is invoked just after Upload starts.
            // Not after successfull upload !!!
            Timber.i("(MyMissionGlobalEventListener) Mission Upload - OK - error is null");
            droneMainController.onMissionUploadResult(null);
            return;
        }
        Timber.e(String.format("(MyMissionGlobalEventListener) Mission Upload - FAILED - %s",
                djiError.getDescription()));
        droneMainController.onMissionUploadResult(djiError);
    }

    @Override
    public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {

    }

    @Override
    public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {
        droneMainController.onMissionUploadUpdate(waypointMissionUploadEvent);
    }

    @Override
    public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {
        WaypointExecutionProgress progress = waypointMissionExecutionEvent.getProgress();
        if (progress == null)
            return;

        int tp = progress.targetWaypointIndex;
        boolean reached = progress.isWaypointReached;
        WaypointMissionExecuteState state = progress.executeState;
        int total = progress.totalWaypointCount;

        boolean changed = false;

        if (djiTargetPoint == null || djiIsWaypointReached == null || djiExecState == null
                || djiTotalPointCnt == null || djiTargetPoint != tp || djiIsWaypointReached != reached
                || djiExecState != state || djiTotalPointCnt != total) {
            djiTargetPoint = tp;
            djiIsWaypointReached = reached;
            djiExecState = state;
            djiTotalPointCnt = total;
            changed = true;
        }

        if (changed) {
            missionProgressStatusChanged();
        }

       /* WaypointMissionState previousState = waypointMissionExecutionEvent.getPreviousState();
        WaypointMissionState currentState = waypointMissionExecutionEvent.getCurrentState();

        Timber.e(String.format(Locale.US, "%s %s %s %d/%d",
                previousState == null ? "NULL" : previousState.getName(),
                currentState.getName(),
                progress == null ? "NULL" : progress.executeState.toString(),
                progress == null ? -1 : progress.targetWaypointIndex,
                progress == null ? -1 : progress.totalWaypointCount
        ));*/

    }

    /**
     * !!! We are not alowed to change VehicleModel model here,
     * The goal is to notify
     */
    private void missionProgressStatusChanged() {

        long time = System.currentTimeMillis();
        if (djiExecState == WaypointMissionExecuteState.INITIALIZING) {
            lastMissionStartedTime = time;
            lastExecuteStateUpdateTime = time;
        }
        // next code is used to generate AppTest code blocks. DON't remove it please.
        /*if (AppUtils.debug) {
            Timber.w(String.format(Locale.US,
                    "cb.onExecutionUpdate(generateExecutionEvent(%d, %s, WaypointMissionExecuteState.%s, %d));  //
                    %d/%d",
                    djiTargetPoint,
                    djiIsWaypointReached ? "true" : "false",
                    djiExecState.toString(),
                    djiTotalPointCnt,
                    time - lastExecuteStateUpdateTime,
                    time - lastMissionStartedTime
            ));
        } */

        Timber.i(String.format(Locale.US, "onExecutionUpdate tp=%d reached=%s state=%s total=%d %d/%dms",
                djiTargetPoint,
                djiIsWaypointReached ? "TRUE" : "FALSE",
                djiExecState.toString(),
                djiTotalPointCnt,
                time - lastExecuteStateUpdateTime,
                time - lastMissionStartedTime
        ));

        lastExecuteStateUpdateTime = time;

        if (djiExecState == WaypointMissionExecuteState.INITIALIZING) {
            // Nothin to do as will be processed in onExecutionStart method.
            return;
        }

        // lastKnownReacedPoint - is reached point known in our model
        // lastKnownMovingToPoint - is target point known in our model

        // Target point changed - means we reached previous point
        if (djiTargetPoint != lastKnownMovingToPoint) {
            if (lastKnownReacedPoint != djiTargetPoint - 1) {
                lastKnownReacedPoint = djiTargetPoint - 1;
                droneMainController.onMissionWpReached(lastKnownReacedPoint);
            }
            lastKnownReacedPoint = djiTargetPoint - 1;
            lastKnownMovingToPoint = djiTargetPoint;
            droneMainController.onMissionWpMovingTo(lastKnownMovingToPoint);
            return;
        }

        // djiIsWaypointReached - we reached target point
        if (djiIsWaypointReached) {
            if (lastKnownReacedPoint != djiTargetPoint) {
                lastKnownReacedPoint = djiTargetPoint;
                droneMainController.onMissionWpReached(lastKnownReacedPoint);
            }
            lastKnownReacedPoint = djiTargetPoint;
            lastKnownMovingToPoint = djiTargetPoint;
            return;
        }
    }

    @Override
    public void onExecutionStart() {
        lastKnownReacedPoint = -1;
        lastKnownMovingToPoint = 0;
        Timber.i("onExecutionStart");
        droneMainController.onMissionExecutionStart();
    }

    @Override
    public void onExecutionFinish(@Nullable DJIError djiError) {
        lastKnownReacedPoint = -1;
        lastKnownMovingToPoint = -1;
        Timber.i(String.format(Locale.US, "onExecutionFinish (%dms)",
                System.currentTimeMillis() - lastMissionStartedTime));
        droneMainController.onMissionExecutionFinish();
    }

}
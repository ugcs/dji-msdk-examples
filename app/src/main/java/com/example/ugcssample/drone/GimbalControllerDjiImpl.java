package com.example.ugcssample.drone;

import android.os.Handler;

import androidx.annotation.Nullable;
import dji.common.error.DJIError;
import dji.common.gimbal.Axis;
import dji.common.gimbal.GimbalMode;
import dji.common.gimbal.ResetDirection;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;
import timber.log.Timber;

/**
 * Gimbal controller for working with gimbal via DJI SDK.
 */
public class GimbalControllerDjiImpl implements GimbalController {

    private static final int PITCH_MAXIMUM_SPEED = 80;
    private static final int YAW_MAXIMUM_SPEED = 80;
    private static final int DELAY_REPEAT_ROTATION_FOR_SPEED_MS = 100;

    private final DroneBridge droneBridge;
    private final Handler workHandler;

    @Nullable
    private Rotation speedRotation;

    /**
     * Create this gimbal controller.
     *
     * @param droneBridge for accessing components of the drone.
     * @param workHandler represents thread for internal work.
     */
    public GimbalControllerDjiImpl(DroneBridge droneBridge, Handler workHandler) {
        this.droneBridge = droneBridge;
        this.workHandler = workHandler;
    }

    @Override
    public void startRotation(final float pitch, final float yaw) {
        workHandler.post(() -> {
            boolean needToStart = !speedRotationRunning();
            float speedMultiplier = 1.0f;
            //noinspection ConstantConditions
            speedRotation = new Rotation.Builder()
                    .mode(RotationMode.SPEED)
                    .pitch(pitch == 0 ? Rotation.NO_ROTATION : pitch * PITCH_MAXIMUM_SPEED * speedMultiplier)
                    .yaw(yaw == 0 ? Rotation.NO_ROTATION : yaw * YAW_MAXIMUM_SPEED * speedMultiplier)
                    .build();
            if (needToStart) {
                startRepeatingRotationForSpeed();
            }
        });
    }

    @Override
    public void stopRotation() {
        workHandler.post(this::stopRepeatingRotationForSpeed);
    }

    @Override
    public void reset() {
        final Aircraft aircraft = droneBridge.getAircraftInstance();
        if (aircraft == null) {
            Timber.w("vehicle is disconnected already");
            return;
        }
        Gimbal gimbal = aircraft.getGimbal();
        if (gimbal == null) {
            Timber.w("Gimbal is disconnected");
            return;
        }
        gimbal.reset(Axis.YAW_AND_PITCH, ResetDirection.CENTER, djiError -> {
            if (djiError == null) {
                Timber.i("reset success");
            } else {
                Timber.i(djiError.getDescription());
            }
        });
    }

    @Override
    public void setFPVMode(GimbalControllerError result) {
        final Aircraft aircraft = droneBridge.getAircraftInstance();
        if (aircraft == null) {
            Timber.w("vehicle is disconnected already");
            return;
        }
        Gimbal gimbal = aircraft.getGimbal();
        if (gimbal == null) {
            Timber.w("Gimbal is disconnected");
            return;
        }
        gimbal.setMode(GimbalMode.FPV, djiError -> {
            if (djiError != null) {
                result.OnResult(djiError.getErrorCode(), djiError.getDescription());
            } else {
                result.OnResult(0, "");
            }
        });
    }

    @Override
    public void setYAWMode(GimbalControllerError result) {
        final Aircraft aircraft = droneBridge.getAircraftInstance();
        if (aircraft == null) {
            Timber.w("vehicle is disconnected already");
            return;
        }
        Gimbal gimbal = aircraft.getGimbal();
        if (gimbal == null) {
            Timber.w("Gimbal is disconnected");
            return;
        }
        gimbal.setMode(GimbalMode.YAW_FOLLOW, djiError -> {
            if (djiError != null) {
                result.OnResult(djiError.getErrorCode(), djiError.getDescription());
            } else {
                result.OnResult(0, "");
            }
        });
    }

    @Override
    public void smoothTrackEnabled(boolean e) {
        final Aircraft aircraft = droneBridge.getAircraftInstance();
        if (aircraft == null) {
            Timber.w("vehicle is disconnected already");
            return;
        }
        Gimbal gimbal = aircraft.getGimbal();
        if (gimbal == null) {
            Timber.w("Gimbal is disconnected");
            return;
        }

        Timber.i("setting to  %s", e);
        gimbal.setYawSimultaneousFollowEnabled(e, djiError -> {
            if (djiError == null) {
                Timber.i("success");
            } else {
                Timber.i(djiError.getDescription());
            }

            aircraft.getGimbal().getYawSimultaneousFollowEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    Timber.i("success %s", aBoolean);
                }

                @Override
                public void onFailure(DJIError djiError) {
                    Timber.i(djiError.getDescription());
                }
            });
        });
    }

    private void rotate(Rotation rotation) {
        Aircraft aircraft = droneBridge.getAircraftInstance();
        if (rotation == null) {
            Timber.e("startRotation - rotation is NULL !!!");
            return;
        }

        Gimbal gimbal = aircraft.getGimbal();
        Camera djiCam = aircraft.getCamera();
        if (gimbal != null && djiCam != null) {
            try {
                gimbal.rotate(rotation, djiError -> {
                    if (djiError != null) {
                        Timber.w("startRotation result = %s", djiError.getDescription());
                    }
                });
            } catch (Exception e) {
                Timber.w("startRotation ERROR");
            }
        } else {
            if (gimbal == null) {
                Timber.e("startRotation - gimbal is NULL !!!");
            }
            if (djiCam == null) {
                Timber.e("startRotation - djiCam is NULL !!!");
            }
        }
    }

    private void startRepeatingRotationForSpeed() {
        workHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (speedRotationRunning()) {
                    rotate(speedRotation);
                    // register this action again.
                    workHandler.postDelayed(this, DELAY_REPEAT_ROTATION_FOR_SPEED_MS);
                }
            }
        }, DELAY_REPEAT_ROTATION_FOR_SPEED_MS);
    }

    private void stopRepeatingRotationForSpeed() {
        if (speedRotation != null) {
            speedRotation = null;

            //noinspection ConstantConditions
            rotate(new Rotation.Builder()
                    .mode(RotationMode.SPEED)
                    .pitch(Rotation.NO_ROTATION)
                    .yaw(Rotation.NO_ROTATION)
                    .roll(Rotation.NO_ROTATION)
                    .build()
            );
        }
    }

    private boolean speedRotationRunning() {
        return speedRotation != null;
    }

    public static float generateSpeedMultiplier(int coef, int min, int max, int cur) {
        float speedMultiplier;
        float slope = ((float) (max - min)) / (coef - 1);
        float coefToSet = ((float) (cur - min)) / slope + 1;
        speedMultiplier = 1 / coefToSet;
        return speedMultiplier;
    }

}

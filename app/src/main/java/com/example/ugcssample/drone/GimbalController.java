package com.example.ugcssample.drone;
/**
 * Provide a way to manipulate with the gimbal.
 */
public interface GimbalController {

    public interface GimbalControllerError {
        void OnResult (int result, String errorDesription);
    }
    /**
     * Start rotating the gimbal in the specified direction.
     * The absolute value corresponds to the speed of rotation.
     * The maximum absolute value corresponds to the maximum speed.
     *
     * @param pitch in range[-1, 1], positive value corresponds to the upward rotation.
     * @param yaw   in range[-1, 1], positive value corresponds to a rotation to the right.
     */
    void startRotation(float pitch, float yaw);

    /**
     * Stops previously started rotation.
     */
    void stopRotation();

    void reset(GimbalControllerError result);

    void setFPVMode(GimbalControllerError result);

    void setYAWMode(GimbalControllerError result);

    void smoothTrackEnabled(boolean e);
}

package com.example.ugcssample.model.utils.unitsystem.providers.speed;

public abstract class SpeedUnitProvider {

    public abstract String getDefLetter();

    // Override this method in ImperialSpeedUnitProvider
    public double getFromMetersPerSecond(double speedMs) {
        return speedMs;
    }

    // Override this method in ImperialSpeedUnitProvider
    public float getFromMetersPerSecond(float speedMs) {
        return speedMs;
    }

    // Override this method in ImperialSpeedUnitProvider
    public double toMetersPerSecond(double valInWhateverSystem) {
        return valInWhateverSystem;
    }

    // Override this method in ImperialSpeedUnitProvider
    public float toMetersPerSecond(float valInWhateverSystem) {
        return valInWhateverSystem;
    }

    public static Double getFromMetersPerSecondOrNull(Double speedMs, SpeedUnitProvider sup) {
        return speedMs == null ? null : sup.getFromMetersPerSecond(speedMs);
    }

}

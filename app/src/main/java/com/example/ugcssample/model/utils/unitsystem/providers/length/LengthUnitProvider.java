package com.example.ugcssample.model.utils.unitsystem.providers.length;

public abstract class LengthUnitProvider {

    // 1m = 3,28084ft
    // 1ft = 0.3048m
    public static final double METERS_IN_FOOT = 0.3048d;
    public static final double METERS_IN_MILE = 1609.34d;

    public abstract String getDefLetter();

    // Override this method in ImperialLengthUnitProvider
    public double getFromMeters(double valInMeters) {
        return valInMeters;
    }

    // Override this method in ImperialLengthUnitProvider
    public float getFromMeters(float valInMeters) {
        return valInMeters;
    }

    // Override this method in ImperialLengthUnitProvider
    public double toMeters(double valInWhateverSystem) {
        return valInWhateverSystem;
    }

    // Override this method in ImperialLengthUnitProvider
    public float toMeters(float valInWhateverSystem) {
        return valInWhateverSystem;
    }

    public static Double getFromMetersOrNull(Double valInMeters, LengthUnitProvider lup) {
        return valInMeters == null ? null : lup.getFromMeters(valInMeters);
    }

    public static Double toMetersOrNull(Double valInWhateverSystem, LengthUnitProvider lup) {
        return valInWhateverSystem == null ? null : lup.toMeters(valInWhateverSystem);
    }
}

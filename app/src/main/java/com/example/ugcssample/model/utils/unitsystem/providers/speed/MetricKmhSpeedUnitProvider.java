package com.example.ugcssample.model.utils.unitsystem.providers.speed;

public class MetricKmhSpeedUnitProvider extends SpeedUnitProvider {

    public static final double MS_IS_KMH = 3.6;

    @Override
    public String getDefLetter() {
        return "km/h";
    }

    @Override
    public double getFromMetersPerSecond(double speed) {
        return speed * MS_IS_KMH;
    }

    @Override
    public float getFromMetersPerSecond(float speed) {
        return (float)(speed * MS_IS_KMH);
    }

    @Override
    public double toMetersPerSecond(double valInWhateverSystem) {
        return valInWhateverSystem / MS_IS_KMH;
    }

    @Override
    public float toMetersPerSecond(float valInWhateverSystem) {
        return (float)(valInWhateverSystem / MS_IS_KMH);
    }

}

package com.example.ugcssample.model.utils.unitsystem.providers.speed;

public class ImperialMphSpeedUnitProvider extends SpeedUnitProvider {

    public static final double MS_IS_MPH = 2.23693629;

    @Override
    public String getDefLetter() {
        return "MPH";
    }

    @Override
    public double getFromMetersPerSecond(double speed) {
        return speed * MS_IS_MPH;
    }

    @Override
    public float getFromMetersPerSecond(float speed) {
        return (float)(speed * MS_IS_MPH);
    }

    @Override
    public double toMetersPerSecond(double valInWhateverSystem) {
        return valInWhateverSystem / MS_IS_MPH;
    }

    @Override
    public float toMetersPerSecond(float valInWhateverSystem) {
        return (float)(valInWhateverSystem / MS_IS_MPH);
    }

}

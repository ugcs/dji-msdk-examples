package com.example.ugcssample.model.utils.unitsystem.providers.speed;


import com.example.ugcssample.model.utils.unitsystem.providers.length.LengthUnitProvider;

public class ImperialFtsSpeedUnitProvider extends SpeedUnitProvider {

    @Override
    public String getDefLetter() {
        return "ft/s";
    }

    @Override
    public double getFromMetersPerSecond(double speed) {
        return speed / LengthUnitProvider.METERS_IN_FOOT;
    }

    @Override
    public float getFromMetersPerSecond(float speed) {
        return (float)(speed / LengthUnitProvider.METERS_IN_FOOT);
    }

    @Override
    public double toMetersPerSecond(double valInWhateverSystem) {
        return valInWhateverSystem * LengthUnitProvider.METERS_IN_FOOT;
    }

    @Override
    public float toMetersPerSecond(float valInWhateverSystem) {
        return (float)(valInWhateverSystem * LengthUnitProvider.METERS_IN_FOOT);
    }

}

package com.example.ugcssample.model.utils.unitsystem.providers.length;

public class ImperialLengthUnitProvider extends LengthUnitProvider {

    @Override
    public String getDefLetter() {
        return "ft";
    }

    @Override
    public double getFromMeters(double valInMeters) {
        return valInMeters / METERS_IN_FOOT;
    }

    @Override
    public float getFromMeters(float valInMeters) {
        return (float)(valInMeters / METERS_IN_FOOT);
    }

    @Override
    public double toMeters(double valInWhateverSystem) {
        return valInWhateverSystem * METERS_IN_FOOT;
    }

    @Override
    public float toMeters(float valInWhateverSystem) {
        return (float)(valInWhateverSystem * METERS_IN_FOOT);
    }

}

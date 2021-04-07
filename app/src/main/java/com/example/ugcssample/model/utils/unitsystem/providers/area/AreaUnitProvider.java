package com.example.ugcssample.model.utils.unitsystem.providers.area;

public abstract class AreaUnitProvider {

    // 1m2 = 10.7639ft2
    // 1ft = 0.092903m2
    public static final double SQ_METERS_IN_SQ_FOOT = 0.092903d;

    public abstract String getDefLetter();

    public double getFromSquareMeters(double valInSquareMeters) {
        return valInSquareMeters;
    }

    // Override this method in ImperialLengthUnitProvider
    public double toSquareMeters(double valInWhateverSystem) {
        return valInWhateverSystem;
    }

}

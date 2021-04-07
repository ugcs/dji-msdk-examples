package com.example.ugcssample.model.utils.unitsystem.providers.area;

public class ImperialAreaUnitProvider extends AreaUnitProvider {

    @Override
    public String getDefLetter() {
        return "ft2";
    }

    @Override
    public double getFromSquareMeters(double valInSquareMeters) {
        return valInSquareMeters / SQ_METERS_IN_SQ_FOOT;
    }

    @Override
    public double toSquareMeters(double valInWhateverSystem) {
        return valInWhateverSystem * SQ_METERS_IN_SQ_FOOT;
    }

}

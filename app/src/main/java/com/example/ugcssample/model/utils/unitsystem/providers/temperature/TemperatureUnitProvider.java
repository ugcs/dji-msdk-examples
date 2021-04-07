package com.example.ugcssample.model.utils.unitsystem.providers.temperature;

public abstract class TemperatureUnitProvider {

    public abstract String getDefLetter();
    
    // Override this method in ImperialLengthUnitProvider
    public double getFromCelsius(double valInCelsius) {
        return valInCelsius;
    }

    // Override this method in ImperialLengthUnitProvider
    public float getFromCelsius(float valInCelsius) {
        return valInCelsius;
    }

    // Override this method in ImperialLengthUnitProvider
    public double toCelsius(double valInWhateverSystem) {
        return valInWhateverSystem;
    }

    // Override this method in ImperialLengthUnitProvider
    public float toCelsius(float valInWhateverSystem) {
        return valInWhateverSystem;
    }

    public static Double getFromCelsiusOrNull(Double valInCelsius, TemperatureUnitProvider tup) {
        return valInCelsius == null ? null : tup.getFromCelsius(valInCelsius);
    }

}

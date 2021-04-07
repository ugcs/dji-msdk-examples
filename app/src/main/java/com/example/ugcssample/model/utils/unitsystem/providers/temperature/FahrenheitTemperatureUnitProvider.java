package com.example.ugcssample.model.utils.unitsystem.providers.temperature;

public class FahrenheitTemperatureUnitProvider extends TemperatureUnitProvider {
    @Override
    public String getDefLetter() {
        return "°F";
    }

    @Override
    public double getFromCelsius(double valInCelsius) {
        return valInCelsius * 9.0 / 5.0 + 32.0;
    }

    @Override
    public float getFromCelsius(float valInCelsius) {
        return valInCelsius * 9F / 5F + 32F;
    }

    @Override
    public double toCelsius(double valInFahrenheit) {
        return (valInFahrenheit - 32.0) * 5.0 / 9.0;
    }

    @Override
    public float toCelsius(float valInFahrenheit) {
        return (valInFahrenheit - 32F) * 5F / 9F;
    }
}

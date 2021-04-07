package com.example.ugcssample.model.utils.unitsystem.systems;


import com.example.ugcssample.model.utils.unitsystem.providers.area.AreaUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.area.ImperialAreaUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.length.ImperialLengthUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.length.LengthUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.temperature.FahrenheitTemperatureUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.temperature.TemperatureUnitProvider;

public abstract class ImperialUnitSystem implements UnitSystem {

    private static final LengthUnitProvider LENGTH_UNIT_PROVIDER = new ImperialLengthUnitProvider();
    private static final AreaUnitProvider AREA_UNIT_PROVIDER = new ImperialAreaUnitProvider();
    private static final TemperatureUnitProvider TEMPERATURE_UNIT_PROVIDER = new FahrenheitTemperatureUnitProvider();

    @Override
    public LengthUnitProvider getLengthUnitProvider() {
        return LENGTH_UNIT_PROVIDER;
    }

    @Override
    public AreaUnitProvider getAreaUnitProvider() {
        return AREA_UNIT_PROVIDER;
    }

    @Override
    public TemperatureUnitProvider getTemperatureUnitProvider() {
        return TEMPERATURE_UNIT_PROVIDER;
    }

}

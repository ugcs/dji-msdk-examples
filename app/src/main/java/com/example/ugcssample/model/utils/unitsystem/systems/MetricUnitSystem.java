package com.example.ugcssample.model.utils.unitsystem.systems;


import com.example.ugcssample.model.utils.unitsystem.providers.area.AreaUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.area.MetricAreaUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.length.LengthUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.length.MetricLengthUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.temperature.CelsiusTemperatureUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.temperature.TemperatureUnitProvider;

public abstract class MetricUnitSystem implements UnitSystem {

    private static final LengthUnitProvider LENGTH_UNIT_PROVIDER = new MetricLengthUnitProvider();
    private static final AreaUnitProvider AREA_UNIT_PROVIDER = new MetricAreaUnitProvider();
    private static final TemperatureUnitProvider TEMPERATURE_UNIT_PROVIDER = new CelsiusTemperatureUnitProvider();

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

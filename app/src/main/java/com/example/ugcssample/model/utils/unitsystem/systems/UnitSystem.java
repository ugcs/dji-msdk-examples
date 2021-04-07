package com.example.ugcssample.model.utils.unitsystem.systems;

import com.example.ugcssample.model.utils.unitsystem.providers.area.AreaUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.length.LengthUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.speed.SpeedUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.temperature.TemperatureUnitProvider;

public interface UnitSystem {

    LengthUnitProvider getLengthUnitProvider();

    SpeedUnitProvider getSpeedUnitProvider();

    AreaUnitProvider getAreaUnitProvider();

    TemperatureUnitProvider getTemperatureUnitProvider();
}

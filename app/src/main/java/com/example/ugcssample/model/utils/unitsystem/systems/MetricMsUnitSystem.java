package com.example.ugcssample.model.utils.unitsystem.systems;


import com.example.ugcssample.model.utils.unitsystem.providers.speed.MetricMsSpeedUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.speed.SpeedUnitProvider;

public class MetricMsUnitSystem extends MetricUnitSystem {

    private static final SpeedUnitProvider SPEED_UNIT_PROVIDER = new MetricMsSpeedUnitProvider();

    @Override
    public SpeedUnitProvider getSpeedUnitProvider() {
        return SPEED_UNIT_PROVIDER;
    }

}

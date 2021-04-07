package com.example.ugcssample.model.utils.unitsystem.systems;

import com.example.ugcssample.model.utils.unitsystem.providers.speed.MetricKmhSpeedUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.speed.SpeedUnitProvider;

public class MetricKmhUnitSystem extends MetricUnitSystem {

    private static final SpeedUnitProvider SPEED_UNIT_PROVIDER = new MetricKmhSpeedUnitProvider();

    @Override
    public SpeedUnitProvider getSpeedUnitProvider() {
        return SPEED_UNIT_PROVIDER;
    }

}

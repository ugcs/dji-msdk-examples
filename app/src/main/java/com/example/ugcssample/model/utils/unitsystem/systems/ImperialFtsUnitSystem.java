package com.example.ugcssample.model.utils.unitsystem.systems;

import com.example.ugcssample.model.utils.unitsystem.providers.speed.ImperialFtsSpeedUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.speed.SpeedUnitProvider;

public class ImperialFtsUnitSystem extends ImperialUnitSystem {

    private static final SpeedUnitProvider SPEED_UNIT_PROVIDER = new ImperialFtsSpeedUnitProvider();

    @Override
    public SpeedUnitProvider getSpeedUnitProvider() {
        return SPEED_UNIT_PROVIDER;
    }

}

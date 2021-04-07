package com.example.ugcssample.model.utils.unitsystem.systems;


import com.example.ugcssample.model.utils.unitsystem.providers.speed.ImperialMphSpeedUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.speed.SpeedUnitProvider;

public class ImperialMphUnitSystem extends ImperialUnitSystem {

    private static final SpeedUnitProvider SPEED_UNIT_PROVIDER = new ImperialMphSpeedUnitProvider();

    @Override
    public SpeedUnitProvider getSpeedUnitProvider() {
        return SPEED_UNIT_PROVIDER;
    }

}

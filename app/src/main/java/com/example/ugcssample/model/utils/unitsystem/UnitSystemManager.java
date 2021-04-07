package com.example.ugcssample.model.utils.unitsystem;

import com.example.ugcssample.model.utils.unitsystem.systems.ImperialFtsUnitSystem;
import com.example.ugcssample.model.utils.unitsystem.systems.ImperialMphUnitSystem;
import com.example.ugcssample.model.utils.unitsystem.systems.MetricKmhUnitSystem;
import com.example.ugcssample.model.utils.unitsystem.systems.MetricMsUnitSystem;
import com.example.ugcssample.model.utils.unitsystem.systems.UnitSystem;

public final class UnitSystemManager {

    private static MetricMsUnitSystem metricMsUnitSystem;
    private static MetricKmhUnitSystem metricKmhUnitSystem;
    private static ImperialFtsUnitSystem imperialFtsUnitSystem;
    private static ImperialMphUnitSystem imperialMphUnitSystem;

    private UnitSystemManager() {
    }

    public static UnitSystem getUnitSystem(UnitSystemType ust) {
        if (ust == UnitSystemType.IMPERIAL) {
            if (imperialFtsUnitSystem == null)
                imperialFtsUnitSystem = new ImperialFtsUnitSystem();
            return imperialFtsUnitSystem;
        } else if (ust == UnitSystemType.IMPERIAL_MPH) {
            if (imperialMphUnitSystem == null)
                imperialMphUnitSystem = new ImperialMphUnitSystem();
            return imperialMphUnitSystem;
        } else if (ust == UnitSystemType.METRIC_KMH) {
            if (metricKmhUnitSystem == null)
                metricKmhUnitSystem = new MetricKmhUnitSystem();
            return metricKmhUnitSystem;
        } else {
            if (metricMsUnitSystem == null)
                metricMsUnitSystem = new MetricMsUnitSystem();
            return metricMsUnitSystem;
        }
    }
}

package com.example.ugcssample.model.utils;

import android.content.Context;

public final class VehicleModelUtils {

    private VehicleModelUtils() {
    }
/*
    public static String getVehicleName(VehicleType type, Context ctx) {
        if (type == null)
            return null;

        switch (type) {
            case GENERIC_COPTER:
                return ctx.getString(R.string.VehicleType_GENERIC_COPTER);
            case EMU_COPTER:
                return ctx.getString(R.string.VehicleType_EMU_COPTER);
            case ARDU_COPTER:
                return ctx.getString(R.string.VehicleType_ARDU_COPTER);
            case PIXHAWK:
                return ctx.getString(R.string.VehicleType_PIXHAWK);
            case EMU_PLANE:
                return ctx.getString(R.string.VehicleType_EMU_PLANE);
            case MICRODRONES:
                return ctx.getString(R.string.VehicleType_MICRODRONES);
            case MIKROKOPTER:
                return ctx.getString(R.string.VehicleType_MIKROKOPTER);
            case INDAGO:
                return ctx.getString(R.string.VehicleType_INDAGO);
            case GENERIC_ROVER:
                return ctx.getString(R.string.VehicleType_GENERIC_ROVER);
            case GENERIC_BOAT:
                return ctx.getString(R.string.VehicleType_GENERIC_BOAT);

            case DJI_PHANTOM2_VISION_PLUS:
                return ctx.getString(R.string.VehicleType_DJI_PHANTOM2_VISION_PLUS);

            case DJI_INSPIRE_1:
                return ctx.getString(R.string.VehicleType_DJI_INSPIRE_1);
            case DJI_INSPIRE_1_PRO:
                return ctx.getString(R.string.VehicleType_DJI_INSPIRE_1_PRO);
            case DJI_INSPIRE_1_RAW:
                return ctx.getString(R.string.VehicleType_DJI_INSPIRE_1_RAW);
            case DJI_INSPIRE_2:
                return ctx.getString(R.string.VehicleType_DJI_INSPIRE_2);

            case DJI_PHANTOM3_PRO:
                return ctx.getString(R.string.VehicleType_DJI_PHANTOM3_PRO);
            case DJI_PHANTOM3_ADV:
                return ctx.getString(R.string.VehicleType_DJI_PHANTOM3_ADV);
            case DJI_PHANTOM3_STA:
                return ctx.getString(R.string.VehicleType_DJI_PHANTOM3_STA);
            case DJI_PHANTOM3_4K:
                return ctx.getString(R.string.VehicleType_DJI_PHANTOM3_4K);

            case DJI_PHANTOM_4:
                return ctx.getString(R.string.VehicleType_DJI_PHANTOM4);
            case DJI_PHANTOM_4_PRO:
                return ctx.getString(R.string.VehicleType_DJI_PHANTOM4_PRO);
            case DJI_PHANTOM_4_ADV:
                return ctx.getString(R.string.VehicleType_DJI_PHANTOM4_ADV);
            case DJI_PHANTOM_4_PRO_V2:
                return ctx.getString(R.string.VehicleType_DJI_PHANTOM_4_PRO_V2);
            case DJI_PHANTOM_4_RTK:
                return ctx.getString(R.string.VehicleType_DJI_PHANTOM_4_RTK);

            case DJI_M100:
                return ctx.getString(R.string.VehicleType_DJI_M100);

            case DJI_M200:
                return ctx.getString(R.string.VehicleType_DJI_M200);
            case DJI_M200_V2:
                return ctx.getString(R.string.VehicleType_DJI_M200_V2);
            case DJI_M210:
                return ctx.getString(R.string.VehicleType_DJI_M210);
            case DJI_M210_RTK:
                return ctx.getString(R.string.VehicleType_DJI_M210_RTK);

            case DJI_M600:
                return ctx.getString(R.string.VehicleType_DJI_M600);
            case DJI_M600_PRO:
                return ctx.getString(R.string.VehicleType_DJI_M600_PRO);

            case DJI_A2:
                return ctx.getString(R.string.VehicleType_DJI_A2);
            case DJI_A3:
                return ctx.getString(R.string.VehicleType_DJI_A3);
            case DJI_N3:
                return ctx.getString(R.string.VehicleType_DJI_N3);
            case DJI_MAVIC_PRO:
                return ctx.getString(R.string.VehicleType_DJI_MAVIC_PRO);
            case DJI_MAVIC_AIR:
                return ctx.getString(R.string.VehicleType_DJI_MAVIC_AIR);
            case DJI_MAVIC_2:
                return ctx.getString(R.string.VehicleType_DJI_MAVIC_2);
            case DJI_MAVIC_2_PRO:
                return ctx.getString(R.string.VehicleType_DJI_MAVIC_2_PRO);
            case DJI_MAVIC_2_ZOOM:
                return ctx.getString(R.string.VehicleType_DJI_MAVIC_2_ZOOM);
            case DJI_MAVIC_2_ENTERPRISE:
                return ctx.getString(R.string.VehicleType_DJI_MAVIC_2_ENTERPRISE);
            case DJI_MAVIC_2_ENTERPRISE_DUAL:
                return ctx.getString(R.string.VehicleType_DJI_MAVIC_2_ENTERPRISE_DUAL);

            case DJI_SPARK:
                return ctx.getString(R.string.VehicleType_DJI_SPARK);
            case DJI_UNKNOWN_AIRCRAFT:
                return ctx.getString(R.string.VehicleType_DJI_UNKNOWN_AIRCRAFT);

            case UNKNOWN:
                return ctx.getString(R.string.VehicleType_UNKNOWN);

            default:
                AppUtils.unhandledSwitch(type.name());
                return null;

        }
    }

    public static String logVehicle(VehicleModel vm) {
        if (vm == null) {
            return "VehicleModel DISCONNECTED";
        }

        return String.format(AppUtils.LOCALE,
            "VehicleModel - %1.7f/%1.7f(%d) %1.2fm att(%1.1f/%1.1f/%1.1f) g-att(%1.1f/%1.1f/%1.1f) %s",
            vm.position.gps.latitude, vm.position.gps.longitude, vm.position.gps.satNumber,
            vm.position.getAltitude(),
            vm.attitude.pitch, vm.attitude.roll, vm.attitude.yaw,
            vm.gimbalAttitude.pitch, vm.gimbalAttitude.roll, vm.gimbalAttitude.yaw,
            vm.missionInfo.droneControlModeNativeName);
    }
*/
}



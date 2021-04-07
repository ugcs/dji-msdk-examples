package com.example.ugcssample.model.utils;

import android.content.res.Resources;


import java.text.SimpleDateFormat;
import java.util.Date;

public final class TimeUtils {

    //private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.US);
    private static final String TIME_FORMAT = "HH:mm:ss.SSS";
    private static final int SECONDS_IN_MINUTE = 60;
    private static final int SECONDS_IN_HOUR = 60 * 60;
    private static final int SECONDS_IN_DAY = 60 * 60 * 24;

    private TimeUtils() {
    }

    public static String durationToHhMmSs(long durationInSeconds) {
        long minutesAndSeconds = durationInSeconds % 3600;
        return String.format(AppUtils.LOCALE, "%02d:%02d:%02d",
            durationInSeconds / 3600, minutesAndSeconds / 60, minutesAndSeconds % 60);
    }

    public static String durationToMmSs(long durationInSeconds) {
        return String.format(AppUtils.LOCALE, "%02d:%02d", durationInSeconds / 60, durationInSeconds % 60);
    }

    public static String formatElapsedTime(Resources resources, int durationInSeconds) {
        if (durationInSeconds < SECONDS_IN_MINUTE) {
            return "resources.getString(R.string.elapsed_time_just_now)";
        } else if (durationInSeconds < SECONDS_IN_HOUR) {
            int minutes = durationInSeconds / SECONDS_IN_MINUTE;
            return "resources.getQuantityString(R.plurals.elapsed_time_minutes, minutes, minutes)";
        } else if (durationInSeconds < SECONDS_IN_DAY) {
            int hours = durationInSeconds / SECONDS_IN_HOUR;
            return "resources.getQuantityString(R.plurals.elapsed_time_hours, hours, hours)";
        } else {
            int days = durationInSeconds / SECONDS_IN_DAY;
            return "resources.getQuantityString(R.plurals.elapsed_time_days, days, days";
        }
    }

    public static String getTimeStamp() {
        return new SimpleDateFormat(TIME_FORMAT, AppUtils.LOCALE).format(new Date());
    }

}

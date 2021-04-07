package com.example.ugcssample.model.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoggerToSysOut implements Logger {

    //public static final String dateTimeFormat = "yyyy-MM-dd HH-mm-ss";
    public static final String DATE_TIME_FORMAT = "HH:mm:ss";

    @Override
    public void d(String tag, String msg) {
        log("D", tag, msg);
    }

    @Override
    public void i(String tag, String msg) {
        log("I", tag, msg);
    }

    @Override
    public void w(String tag, String msg) {
        log("W", tag, msg);
    }

    @Override
    public void e(String tag, String msg) {
        log("E", tag, msg);
    }

    @Override
    public void printStackTrace(String tag, Exception e) {
        if (e != null) {
            log("E", tag, e.getMessage());
            for (StackTraceElement el : e.getStackTrace()) {
                log("E", tag, el.getMethodName());
            }
        }
    }

    @Override
    public void printStackTrace(String tag, Throwable e) {
        if (e != null) {
            log("E", tag, e.getMessage());
            for (StackTraceElement el : e.getStackTrace()) {
                log("E", tag, el.getMethodName());
            }
        }
    }

    private void log(String level, String tag, String msg) {
        System.out.println(String.format("%s: %s %s - %s",
            new SimpleDateFormat(DATE_TIME_FORMAT, Locale.US).format(new Date()),
            level, tag, msg));
    }
}

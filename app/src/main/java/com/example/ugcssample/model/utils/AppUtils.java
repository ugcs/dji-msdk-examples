package com.example.ugcssample.model.utils;

import android.os.Handler;
import android.os.Looper;


import com.example.ugcssample.model.utils.threads.MyScheduledExecutorService;

import java.util.Locale;

public final class AppUtils {

    private static final String TAG = AppUtils.class.getSimpleName();

    public static final String UNSUPPORTED_OPERATION = "Unsupported operation ";
    public static final String UNHANDLED_SWITCH = "Unhandled switch ";
    public static final String NON_NULL = "Parameter should not be NULL";
    public static final String FORBIDDEN_METHOD = "Forbidden method";
    private static final String ACTIVITY_IMPL_ERROR = "Activity %s must implement %s interface";
    public static final String INVALID_ARGUMENT = "Invalid argument value";

    public static boolean debug = false;
    public static boolean publicDebug = false;

    public static final Locale LOCALE = Locale.US;

    private AppUtils() {
    }

    public static void crashApplication(final MyScheduledExecutorService executorService, Exception e) {
        final Logger logger = executorService.getLogger();
        final String executorServiceName = executorService.getExecutorServiceName();
        if (logger != null) {
            logger.e(TAG, executorServiceName + " is going to crashApplication...");
            logger.printStackTrace(TAG, e);
        }
        RuntimeException exception;
        if (e == null || e.getMessage() == null) {
            exception = new RuntimeException("No Exception Reason Provided");
        } else {
            exception = new RuntimeException(e.getMessage());
        }
        if (logger != null) {
            logger.printStackTrace(TAG, exception);
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (logger != null) {
                    logger.w(TAG, "crashApplication NOW. Buy!");
                }
                throw exception;
            }
        }, 1000);
    }

    public static void crashApplication(final String reason) {
        final RuntimeException exception = new RuntimeException(reason);
        exception.printStackTrace();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                throw exception;
            }
        });
    }

    public static void unhandledSwitch(String key) {
        AppUtils.crashApplication(UNHANDLED_SWITCH + key);
    }

    public static void forbiddenMethod(String key) {
        AppUtils.crashApplication(FORBIDDEN_METHOD + key);
    }

    public static void activityMustImplement(Class<?> activityClass, Class<?> mustImplementClass) {
        String reason = String.format(AppUtils.ACTIVITY_IMPL_ERROR,
            activityClass.getName(), mustImplementClass.getName());
        AppUtils.crashApplication(reason);
    }
}

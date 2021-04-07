package com.example.ugcssample.model.utils;

public interface Logger {

    void d(String tag, String msg);

    void i(String tag, String msg);

    void w(String tag, String msg);

    void e(String tag, String msg);

    void printStackTrace(String tag, Exception e);

    void printStackTrace(String tag, Throwable e);
}

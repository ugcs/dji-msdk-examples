package com.example.ugcssample.model.utils;

public final class IntegerUtils {

    private IntegerUtils() {
        // Utility class
    }

    public static boolean equals(Integer i, Integer j) {
        return (i == null && j == null) || (i != null && i.equals(j));
    }

}

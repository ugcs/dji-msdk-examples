package com.example.ugcssample.model.utils;


import com.example.ugcssample.model.dto.IntPair;

public final class AspectRatioUtils {

    private AspectRatioUtils() {
        // utility class
    }

    public static IntPair getScaledDimension(IntPair size, IntPair boundary) {
        double xRate = (double)boundary.x / (double)size.x;
        double yRate = (double)boundary.y / (double)size.y;

        if (xRate > yRate) {
            int x = (int)((double)size.x * yRate);
            int y = (int)((double)size.y * yRate);
            return new IntPair(x, y);
        } else {
            int x = (int)((double)size.x * xRate);
            int y = (int)((double)size.y * xRate);
            return new IntPair(x, y);
        }
    }
}

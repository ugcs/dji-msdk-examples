package com.example.ugcssample.model.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ArrayUtils {

    public static class Count<T> {
        private final Map<T, Integer> counter = new HashMap<>();

        public void add(T val) {
            Integer cnt = counter.get(val);
            cnt = cnt == null ? 1 : cnt + 1;
            counter.put(val, cnt);
        }

        public Set<Map.Entry<T, Integer>> result() {
            return counter.entrySet();
        }
    }

    private ArrayUtils() {
        // Utility class
    }

    public static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    public static <T> List<T> copy(List<T> src) {
        if (src == null)
            return null;
        List<T> retVal = new ArrayList<T>(src.size());
        for (T element : src) {
            retVal.add(element);
        }
        return retVal;
    }

    public static double[] copy(double[] src) {
        if (src == null)
            return null;
        double[] retVal = new double[src.length];
        for (int i = 0; i < src.length; i++) {
            retVal[i] = src[i];
        }
        return retVal;
    }

    public static double[] newDouble(int size, double val) {
        double[] retVal = new double[size];
        for (int i = 0; i < size; i++) {
            retVal[i] = val;
        }
        return retVal;
    }

    public static boolean[] newBoolean(int size, boolean val) {
        boolean[] retVal = new boolean[size];
        for (int i = 0; i < size; i++) {
            retVal[i] = val;
        }
        return retVal;
    }

    public static void setBoolean(boolean[] retVal, boolean val) {
        for (int i = 0; i < retVal.length; i++) {
            retVal[i] = val;
        }
    }

    public static Double findMax(double[] values) {
        Double d = null;
        if (values != null) {
            for (double v : values) {
                if (d == null || d < v)
                    d = v;
            }
        }
        return d;
    }

    public static Double findMin(double[] values) {
        Double d = null;
        if (values != null) {
            for (double v : values) {
                if (d == null || d > v)
                    d = v;
            }
        }
        return d;
    }

    public static double findClosestYawRad(double[] in, double of) {
        if (in == null || in.length == 0)
            return of;

        double min = Double.MAX_VALUE;
        double closest = of;
        double diff;

        for (double v : in) {
            diff = Math.abs(MathUtils.findMinimalYawAngleRad(of, v));
            if (diff < min) {
                min = diff;
                closest = v;
            }
        }
        return closest;
    }

    public static List<Double> clone(ArrayList<Double> src) {
        if (src == null) {
            return null;
        }
        return new ArrayList<>(src);
    }

    public static void copy(double[] origin, double[] target, double valueToAdd) {
        for (int i = 0; i < origin.length; i++) {
            target[i] = origin[i] + valueToAdd;
        }
    }

}

package com.example.ugcssample.model.utils;

public final class Preconditions {

    private Preconditions() {
    }

    public static void checkNotNull(Object reference) {
        checkNotNull(reference, null);
    }

    public static void checkNotNull(Object reference, String message) {
        if (reference == null) {
            throw new NullPointerException(message != null
                ? message : "Object reference is null");
        }
    }

    public static void checkNotEmpty(String str) {
        checkNotEmpty(str, null);
    }

    public static void checkNotEmpty(String str, String message) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException(message != null
                ? message : "String is empty");
        }
    }

    public static void checkArgument(boolean condition) {
        checkArgument(condition, null);
    }

    public static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message != null
                ? message : AppUtils.INVALID_ARGUMENT);
        }
    }

    public static void checkIndex(int index, int length) {
        checkIndex(index, length, null);
    }

    public static void checkIndex(int index, int length, String message) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(message != null
                ? message : "Index " + index + " is out of range [0, " + length + ")");
        }
    }

    public static void checkIndexRange(int rangeOffset, int rangeLength, int length) {
        checkIndexRange(rangeOffset, rangeLength, length, null);
    }

    public static void checkIndexRange(int rangeOffset, int rangeLength, int length, String message) {
        if (rangeOffset < 0 || rangeLength < 0 || rangeOffset + rangeLength > length) {
            throw new IndexOutOfBoundsException(message != null
                ? message
                : "Interval [" + rangeOffset + ", " + (rangeOffset + rangeLength)
                + ") is out of range [0, " + length + ")");
        }
    }

}

package com.example.ugcssample.model;

public enum FlightPathType {

    /**
     * Vehicle must reach WP coordinates as the path will be straight lines
     * from one point to the next.
     */
    POINT_TO_POINT,

    /**
     * The WP coordinates may never be reached as the path will be curved.
     */
    CURVED
}

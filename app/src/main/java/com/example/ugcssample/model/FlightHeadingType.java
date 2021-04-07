package com.example.ugcssample.model;

public enum FlightHeadingType {

    /**
     * Vehicle will execute yaw action at WP.
     */
    YAW_AT_WP,

    /**
     * Aircraft's heading will be gradually set to the next waypoint heading while travelling
     * between two adjacent waypoints.
     */
    POI_BETWEEN_WP

}

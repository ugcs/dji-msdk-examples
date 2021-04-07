package com.example.ugcssample.model.dto;

public class PointXyz {
    public double x;
    public double y;
    public double z;

    public PointXyz() {
        this(0.0, 0.0, 0.0);
    }

    public PointXyz(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

}

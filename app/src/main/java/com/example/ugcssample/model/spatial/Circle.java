package com.example.ugcssample.model.spatial;

import android.os.Parcel;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;
import com.example.ugcssample.model.coordinate.LatLongAlt;

public class Circle extends BaseSpatialItem implements android.os.Parcelable {
    public static final Creator<Circle> CREATOR = new Creator<Circle>() {
        public Circle createFromParcel(Parcel source) {
            return new Circle(source);
        }

        public Circle[] newArray(int size) {
            return new Circle[size];
        }
    };
    private double radius = 10;
    private int turns = 1;
    private boolean cw = true;

    public Circle() {
        super(MissionItemType.CIRCLE);
    }

    public Circle(double lat, double lon, double alt) {
        this();
        setCoordinate(new LatLongAlt(lat, lon, alt));
    }

    public Circle(Circle copy) {
        super(copy);
        this.radius = copy.radius;
        this.turns = copy.turns;
        this.cw = copy.cw;
    }

    private Circle(Parcel in) {
        super(in);
        this.radius = in.readDouble();
        this.turns = in.readInt();
        this.cw = in.readInt() > 0;
    }

    public double getRadius() {
        return radius;
    }

    public Circle setRadius(double radius) {
        this.radius = radius;
        return this;
    }

    public int getTurns() {
        return turns;
    }

    public Circle setTurns(int turns) {
        this.turns = turns;
        return this;
    }

    public boolean isCw() {
        return cw;
    }

    public Circle setCw(boolean cw) {
        this.cw = cw;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeDouble(this.radius);
        dest.writeInt(this.turns);
        dest.writeInt(this.cw ? 1 : 0);
    }

    @Override
    public MissionItem cloneMe() {
        return new Circle(this);
    }

}

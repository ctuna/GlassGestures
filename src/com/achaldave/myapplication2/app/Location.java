package com.achaldave.myapplication2.app;

import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by Achal on 4/3/14.
 */
public class Location implements Comparable<Location> {
    public int id;
    public Orientation orientation;

    public Location(int id, Orientation orientation) {
        setOrientation(orientation);
        this.id = id;
    }
    public int getId() {
        return id;
    }

    public Location(Location other) {
        Log.d("Finder", "Cloning location");
        setOrientation(other.orientation);
        this.id = other.id;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = new Orientation(orientation);
    }

    @Override
    public int compareTo(Location other) {
        return this.orientation.compareTo(other.orientation);
    }

}

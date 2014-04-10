package com.achaldave.myapplication2.app;

/**
 * Created by Achal on 4/9/14.
 */
public class Orientation implements Comparable<Orientation> {
    float pitch;
    float yaw;

    public Orientation() { }

    public Orientation(float pitch, float yaw) {
        setOrientation(pitch, yaw);
    }

    public Orientation(Orientation other) {
        setOrientation(other.pitch, other.yaw);
    }

    public Orientation(String serialized) {
        String[] split = serialized.split("\\|");
        float pitch = Float.parseFloat(split[0]);
        float yaw = Float.parseFloat(split[1]);
        setOrientation(pitch, yaw);
    }

    public void setOrientation(float pitch, float yaw) {
        this.pitch = normalize(pitch);
        this.yaw = normalize(yaw);
    }
    public double distance(Orientation other) {
        float yawDist = Orientation.normalize(this.yaw - other.yaw);
        float pitchDist = Orientation.normalize(this.pitch - other.pitch);
        return Math.sqrt(square(this.yaw - other.yaw) + square(this.pitch - other.pitch));
    }

    public static float square(float x) { return x * x; }

    @Override
    public int compareTo(Orientation other) {
        /**
         * TODO: this might need to be negated for left to right
         *
         * Can't tell without testing.
         */
        return Math.round(this.yaw - other.yaw);
    }

    /**
     * Make x range between -PI and PI.
     */
    public static float normalize(float theta) {
        if (theta > Math.PI || theta < -Math.PI)
            theta = 2*((float)Math.PI) - theta;
        return theta;
    }

    public String serialize() {
        return String.format("%f|%f", pitch, yaw);
    }
}

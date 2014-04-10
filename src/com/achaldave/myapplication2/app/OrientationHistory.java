package com.achaldave.myapplication2.app;

import android.Manifest;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Achal on 4/8/14.
 */
public class OrientationHistory {
    /* Determined empirically (aka arbitrarily). */
    private final static float MOVE_THRESHOLD = (float) 0.003;
    /* The raw data that we get; we can get rid of this if not necessary later. */
    private ArrayList<Orientation> rawOrientations;
    /* Smoothed orientations. */
    private ArrayList<Orientation> orientations;
    private int head;
    private int bufferSize;

    public OrientationHistory(int historyLength) {
        this.rawOrientations = new ArrayList<Orientation>(historyLength);
        this.orientations = new ArrayList<Orientation>(historyLength);
        this.bufferSize = historyLength;
        this.head = 0;
    }

    public void add(Orientation orientation) {
        // Log.d("Movement", String.format("head: %d", head));
        if (rawOrientations.size() < (head + 1))
            rawOrientations.add(head, orientation);
        else
            rawOrientations.set(head, orientation);

        Orientation prev = getRaw(-1);

        float smoothPitch = (orientation.pitch + prev.pitch) / 2;
        float smoothYaw = (orientation.yaw + prev.yaw) / 2;
        if (orientations.size() < (head + 1))
            orientations.add(head, orientation);
        else
            orientations.set(head, new Orientation(smoothPitch, smoothYaw));
        head = (head + 1) % bufferSize;
    }

    public Orientation get(int idx) {
        return getHelper(this.orientations, idx);
    }

    /**
     * Look at the variance of the smoothed history; if it's above a threshold, we say we've moved.
     * Yes, this is not the smartest thing to do.
     * @return Whether there is movement in the history we have stored.
     */
    public boolean hasMovement() {
        float variance = getVariance();
        // Log.d("Movement", String.format("Variance: %f", variance));
        return variance > MOVE_THRESHOLD;
    }

    private float getVariance() {
        /* TODO: There's a way to calculate variance in one pass. */
        Orientation avg = getAverage();
        float variance = 0;
        for (Orientation currOrnt : orientations) {
            float dist = (float) currOrnt.distance(avg);
            // Log.d("Movement", String.format("D_avg: (%f, %f), avg(%f, %f), %f", currOrnt.pitch, currOrnt.yaw, avg.pitch, avg.yaw, dist));
            variance += dist;
        }
        return variance / orientations.size();
    }

    private Orientation getAverage () {
        float avgYaw = 0;
        float avgPitch = 0;
        for (Orientation currOrnt : orientations) {
            // Log.d("Movement", String.format("o: (%f, %f)", currOrnt.pitch, currOrnt.yaw));
            avgYaw += currOrnt.yaw;
            avgPitch += currOrnt.pitch;
        }
        avgPitch /= orientations.size();
        avgYaw /= orientations.size();
        // Log.d("Movement", String.format("Average: (%f, %f)", avgPitch, avgYaw));
        return new Orientation(avgPitch, avgYaw);
    }

    private Orientation getRaw(int idx) {
        return getHelper(this.rawOrientations, idx);
    }

    private Orientation getHelper(ArrayList<Orientation> list, int idx) {
        if (idx >= list.size())
            throw new IndexOutOfBoundsException();
        int toAccess = (head + idx) % (bufferSize - 1);
        // Log.d("Movement", String.format("toAccess: %d, list size: %d", toAccess, list.size()));
        if (toAccess < 0) {
            toAccess += list.size();
        }
        return list.get(toAccess);
    }

    private float square(float x) { return x*x; }

}

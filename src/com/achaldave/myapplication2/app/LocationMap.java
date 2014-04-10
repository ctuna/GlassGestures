package com.achaldave.myapplication2.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Created by Achal on 4/7/14.
 */
public class LocationMap {
    private ArrayList<Location> sortedLocations;
    
    private final float DIST_THRESHOLD = 0.1f;
    public LocationMap() {
        sortedLocations = new ArrayList<Location>();
    }

    public Location getById(int id) {
        int idx = getIdxById(id);
        if (idx < 0) return null;
        return getByIndex(getIdxById(id));
    }

    public int getIdxById(int id) {
        for (int i = 0; i < sortedLocations.size(); ++i) {
            if (sortedLocations.get(i).id == id)
                return i;
        }
        return -1;
    }
    public Location getByIndex(int idx) {
        if (sortedLocations.size() == 0) return null;
        return sortedLocations.get(idx);
    }

    public void add(Location location) {
        int idx = getIdxById(location.id);
        if (idx < 0) {
            sortedLocations.add(location);
        } else {
            sortedLocations.set(idx, location);
        }
        sortLocations();
    }

    public void update(Location updated) {
        Location old = getById(updated.id);
        if (old == null) {
            add(updated);
            return;
        }
        float oldYaw = old.orientation.yaw;
        float oldPitch = old.orientation.pitch;
        float updatedYaw = updated.orientation.yaw;
        float updatedPitch = updated.orientation.pitch;
        for (Location loc : sortedLocations) {
            if (loc.id == updated.id) {
                loc.setOrientation(updated.orientation);
                continue;
            }

            float yawRelative = loc.orientation.yaw - oldYaw;
            float pitchRelative = loc.orientation.pitch - oldPitch;
            float currUpdatedYaw = (updatedYaw + yawRelative);
            float currUpdatedPitch = (updatedPitch + pitchRelative);

            currUpdatedYaw = Orientation.normalize(currUpdatedYaw);
            currUpdatedPitch = Orientation.normalize(currUpdatedPitch);

            loc.setOrientation(new Orientation(currUpdatedPitch, currUpdatedYaw));
        }
        sortLocations();
    }

    /**
     * Update @updated devices so that their average falls on the new @average.
     *
     * @param updated Devices that have been updated.
     * @param average New average orientation of those devices.
     */
    public void update(ArrayList<Integer> updated, Orientation average) {
        Orientation oldAverage = new Orientation();
        for (int id : updated) {
            Location curr = getById(id);
            oldAverage.yaw += curr.orientation.yaw;
            oldAverage.pitch += curr.orientation.pitch;
        }
        oldAverage.yaw /= updated.size();
        oldAverage.pitch /= updated.size();

        /* Update the first device, then use that to update the rest. */
        Location first = getById(updated.get(0));
        Location firstNew = new Location(first);
        firstNew.orientation.yaw = average.yaw + (first.orientation.yaw - oldAverage.yaw);
        firstNew.orientation.pitch = average.pitch + (first.orientation.pitch - oldAverage.pitch);

        /* Update the rest of the devices; if we assume the location of any one device, the rest
         * will follow. */
        update(firstNew);
     }

    public int getMatch(OrientationHistory orientations) {
        Orientation orientation = orientations.get(-1);
        if (sortedLocations.size() < 1) return -1;
        double bestDistance = Float.MAX_VALUE;
        int closestId = -1;
        for (int i = 0; i < sortedLocations.size(); ++i) {
            Location loc = sortedLocations.get(i);
            double currDistance = loc.orientation.distance(orientation);
            if (currDistance < bestDistance) {
                bestDistance = currDistance;
                closestId = loc.id;
            }
        }
        if (bestDistance > DIST_THRESHOLD) return -1;
        return closestId;
    }

    public void sortLocations() {
        Collections.sort(sortedLocations);
    }

    public void saveMap(Activity ctx) {
        SharedPreferences prefs = ctx.getPreferences(ctx.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (Location loc : sortedLocations) {
            editor.putString(Integer.toString(loc.id), loc.orientation.serialize());
        }
        editor.commit();
    }

    public void loadMap(Activity ctx) {
        SharedPreferences prefs = ctx.getPreferences(ctx.MODE_PRIVATE);
        Map<String,?> locations = prefs.getAll();
        if (locations.size() <= 0) return;
        for (Map.Entry<String, ?> locationStr : locations.entrySet()) {
            int id = Integer.parseInt(locationStr.getKey());
            Orientation orientation = new Orientation(locationStr.getValue().toString());
            add(new Location(id, orientation));
        }
    }
}

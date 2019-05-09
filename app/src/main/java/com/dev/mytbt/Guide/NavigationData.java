package com.dev.mytbt.Guide;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.oscim.core.GeoPoint;

/**
 * Created by Luís Henriques for MyTbt
 * 16-09-2018, 22:35
 *
 * Model for navigation and positional data
 */

class NavigationData {

    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "tbtNavData";

    // User data
    @NonNull private GeoPoint lastKnownUserPosition;
    private float lastKnownUserBearing;
    private NavPath currentPath;

    NavigationData() {
        lastKnownUserPosition = new GeoPoint(0,0); // guaranteeing we never return null
    }

    /* Sets
    ------------------------------------------ */
    void updateUserPosition(GeoPoint updatedUserPosition, float updatedUserBearing) {
        lastKnownUserPosition = updatedUserPosition;
        lastKnownUserBearing = updatedUserBearing;
    }

    /**
     * Sets a new path instance on the model
     * @param currentPath the new path to be held by this model
     */
    void setCurrentPath(NavPath currentPath) {
        this.currentPath = currentPath;
    }

    /* Gets
    ------------------------------------------ */
    @NonNull
    GeoPoint getLastKnownUserPosition() {
        return lastKnownUserPosition;
    }

    float getLastKnownUserBearing() {
        return lastKnownUserBearing;
    }

    /**
     * Checks whether or not the user is following a path
     * @return returns true is the user is following a valid path
     */
    boolean hasPath() {
        return currentPath != null && !currentPath.getPathPoints().isEmpty();
    }

    /**
     * Gets the current path
     * @return the current path object. May return null if no path is held by this model
     */
    @Nullable NavPath getCurrentPath() {
        return currentPath;
    }
}

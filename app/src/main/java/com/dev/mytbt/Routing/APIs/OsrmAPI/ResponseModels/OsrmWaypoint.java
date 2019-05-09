package com.dev.mytbt.Routing.APIs.OsrmAPI.ResponseModels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 16-04-2018, 12:26
 */
@SuppressWarnings("UnusedDeclaration")
public class OsrmWaypoint {

    @SerializedName("location")
    @Expose
    private double[] location;

    public OsrmWaypoint() {
    }

    public double[] getLocation() {
        return location;
    }

    public void setLocation(double[] location) {
        this.location = location;
    }
}

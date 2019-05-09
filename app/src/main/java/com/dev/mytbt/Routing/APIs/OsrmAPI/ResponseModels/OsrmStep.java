package com.dev.mytbt.Routing.APIs.OsrmAPI.ResponseModels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 16-04-2018, 17:50
 */
@SuppressWarnings("UnusedDeclaration")
public class OsrmStep {

    @SerializedName("maneuver")
    @Expose
    private OsrmManeuver maneuver;

    @SerializedName("name")
    @Expose
    private String streetName = "";

    @SerializedName("distance")
    @Expose
    private String distance = "0";

    public OsrmStep() {
    }

    public OsrmManeuver getManeuver() {
        return maneuver;
    }

    public void setManeuver(OsrmManeuver maneuver) {
        this.maneuver = maneuver;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }
}

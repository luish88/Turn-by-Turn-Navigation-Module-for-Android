package com.dev.mytbt.Routing.APIs.BingAPI.Models.SnapToRoads;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 23-03-2018, 17:28
 */
@SuppressWarnings("UnusedDeclaration")
public class BingSnapCoordinate {

    @SerializedName("latitude")
    @Expose
    private double latitude;

    @SerializedName("longitude")
    @Expose
    private double longitude;

    public BingSnapCoordinate() {
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}

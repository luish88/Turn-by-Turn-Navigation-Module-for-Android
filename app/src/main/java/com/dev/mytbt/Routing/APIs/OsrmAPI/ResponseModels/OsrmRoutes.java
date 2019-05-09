package com.dev.mytbt.Routing.APIs.OsrmAPI.ResponseModels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 16-04-2018, 12:25
 */
@SuppressWarnings("UnusedDeclaration")
public class OsrmRoutes {

    @SerializedName("geometry")
    @Expose
    private OsrmGeometry geometry;

    @SerializedName("legs")
    @Expose
    private OsrmLegs[] legs;

    @SerializedName("distance")
    @Expose
    private String distance;

    @SerializedName("duration")
    @Expose
    private float duration;

    public OsrmRoutes() {
    }

    public OsrmGeometry getGeometry() {
        return geometry;
    }

    public void setGeometry(OsrmGeometry geometry) {
        this.geometry = geometry;
    }

    public OsrmLegs[] getLegs() {
        return legs;
    }

    public void setLegs(OsrmLegs[] legs) {
        this.legs = legs;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }
}

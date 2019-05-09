package com.dev.mytbt.Routing.APIs.BingAPI.Models.SnapToRoads;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 */
@SuppressWarnings("UnusedDeclaration")
public class BingSnappedPoint {

    @SerializedName("coordinate")
    @Expose
    private BingSnapCoordinate coordinates;

    @SerializedName("index")
    @Expose
    private int index;

    @SerializedName("name")
    @Expose
    private String streetName;

    @SerializedName("speedLimit")
    @Expose
    private int speedLimit;

    @SerializedName("speedUnit")
    @Expose
    private String speedUnit;

    public BingSnappedPoint() {
    }

    public BingSnapCoordinate getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(BingSnapCoordinate coordinates) {
        this.coordinates = coordinates;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public int getSpeedLimit() {
        return speedLimit;
    }

    public void setSpeedLimit(int speedLimit) {
        this.speedLimit = speedLimit;
    }

    public String getSpeedUnit() {
        return speedUnit;
    }

    public void setSpeedUnit(String speedUnit) {
        this.speedUnit = speedUnit;
    }
}

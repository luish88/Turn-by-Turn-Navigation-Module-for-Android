package com.dev.mytbt.Routing.APIs.BingAPI.Models.SnapToRoads;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 23-03-2018, 17:18
 */
@SuppressWarnings("UnusedDeclaration")
public class BingSnapResource {

    @SerializedName("snappedPoints")
    @Expose
    private BingSnappedPoint[] snappedPoints;

    public BingSnapResource() {
    }

    public BingSnappedPoint[] getSnappedPoints() {
        return snappedPoints;
    }

    public void setSnappedPoints(BingSnappedPoint[] snappedPoints) {
        this.snappedPoints = snappedPoints;
    }

}

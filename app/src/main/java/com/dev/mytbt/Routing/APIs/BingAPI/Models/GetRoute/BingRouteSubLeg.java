package com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 17-01-2018, 13:22
 */
@SuppressWarnings("UnusedDeclaration")
public class BingRouteSubLeg {


    @SerializedName("endWaypoint")
    @Expose
    private BingEndWaypoint endWaypoint;

    public BingRouteSubLeg() {
    }

    public BingEndWaypoint getEndWaypoint() {
        return endWaypoint;
    }

    public void setEndWaypoint(BingEndWaypoint endWaypoint) {
        this.endWaypoint = endWaypoint;
    }
}

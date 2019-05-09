package com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 17-01-2018, 13:26
 */
@SuppressWarnings("UnusedDeclaration")
public class BingEndWaypoint {

    @SerializedName("routePathIndex")
    @Expose
    private int routePathIndex;

    public BingEndWaypoint() {
    }

    public int getRoutePathIndex() {
        return routePathIndex;
    }

    public void setRoutePathIndex(int routePathIndex) {
        this.routePathIndex = routePathIndex;
    }
}

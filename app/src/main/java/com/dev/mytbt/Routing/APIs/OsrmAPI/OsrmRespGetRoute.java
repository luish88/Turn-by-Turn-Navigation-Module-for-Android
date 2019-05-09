package com.dev.mytbt.Routing.APIs.OsrmAPI;

import com.dev.mytbt.Routing.APIs.OsrmAPI.ResponseModels.OsrmRoutes;
import com.dev.mytbt.Routing.APIs.OsrmAPI.ResponseModels.OsrmWaypoint;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 16-04-2018, 12:11
 */
@SuppressWarnings("UnusedDeclaration")
public class OsrmRespGetRoute {

    @SerializedName("routes")
    @Expose
    private OsrmRoutes[] routes;

    @SerializedName("waypoints")
    @Expose
    private OsrmWaypoint[] waypoints;

    public OsrmRespGetRoute() {
    }

    public OsrmRoutes[] getRoutes() {
        return routes;
    }

    public void setRoutes(OsrmRoutes[] routes) {
        this.routes = routes;
    }

    public OsrmWaypoint[] getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(OsrmWaypoint[] waypoints) {
        this.waypoints = waypoints;
    }
}

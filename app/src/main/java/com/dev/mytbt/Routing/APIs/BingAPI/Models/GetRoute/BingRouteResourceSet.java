package com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 22-12-2017, 11:29
 *
 * Model for Bing API
 * BingRouteResourceSet > BingRouteResource > BingRouteLeg > BingItineraryItem > BingInstruction + BingManeuverPoint + BingWarning
 *                                  > BingRoutePath > BingLine
 */
@SuppressWarnings("UnusedDeclaration")
public class BingRouteResourceSet {

    @SerializedName("resources")
    @Expose
    private BingRouteResource[] resources;

    public BingRouteResourceSet() {
    }

    public BingRouteResource[] getResources() {
        return resources;
    }

    public void setResources(BingRouteResource[] resources) {
        this.resources = resources;
    }
}
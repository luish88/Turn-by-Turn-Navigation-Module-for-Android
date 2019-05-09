package com.dev.mytbt.Routing.APIs.BingAPI.Responses;


import com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute.BingRouteResourceSet;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 20-11-2017, 14:14
 */
@SuppressWarnings("UnusedDeclaration")
public class BingRespGetRoute {

    @SerializedName("resourceSets")
    @Expose
    private BingRouteResourceSet[] bingRouteResourceSets;

    public BingRespGetRoute() {
    }

    public BingRouteResourceSet[] getBingRouteResourceSets() {
        return bingRouteResourceSets;
    }

    public void setBingRouteResourceSets(BingRouteResourceSet[] bingRouteResourceSets) {
        this.bingRouteResourceSets = bingRouteResourceSets;
    }
}

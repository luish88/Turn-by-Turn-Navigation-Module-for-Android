package com.dev.mytbt.Routing.APIs.BingAPI.Responses;

import com.dev.mytbt.Routing.APIs.BingAPI.Models.SnapToRoads.BingSnapResourceSet;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 23-03-2018, 17:05
 */
@SuppressWarnings("UnusedDeclaration")
public class BingRespSnapToRoad {

    @SerializedName("resourceSets")
    @Expose
    private BingSnapResourceSet[] bingSnapResourceSets;

    public BingRespSnapToRoad() {
    }

    public BingSnapResourceSet[] getBingSnapResourceSets() {
        return bingSnapResourceSets;
    }

    public void setBingSnapResourceSets(BingSnapResourceSet[] bingSnapResourceSets) {
        this.bingSnapResourceSets = bingSnapResourceSets;
    }
}

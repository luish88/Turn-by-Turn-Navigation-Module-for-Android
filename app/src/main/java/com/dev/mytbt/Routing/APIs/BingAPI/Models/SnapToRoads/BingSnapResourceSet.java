package com.dev.mytbt.Routing.APIs.BingAPI.Models.SnapToRoads;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 23-03-2018, 17:15
 */
@SuppressWarnings("UnusedDeclaration")
public class BingSnapResourceSet {

    @SerializedName("resources")
    @Expose
    private BingSnapResource[] resources;

    public BingSnapResourceSet() {
    }

    public BingSnapResource[] getResources() {
        return resources;
    }

    public void setResources(BingSnapResource[] resources) {
        this.resources = resources;
    }
}

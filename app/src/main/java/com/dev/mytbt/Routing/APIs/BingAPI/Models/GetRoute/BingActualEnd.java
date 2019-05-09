package com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 17-01-2018, 11:53
 */
@SuppressWarnings("UnusedDeclaration")
public class BingActualEnd {

    @SerializedName("coordinates")
    @Expose
    private Double[] coordinates;

    public BingActualEnd() {
    }

    public Double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Double[] coordinates) {
        this.coordinates = coordinates;
    }
}

package com.dev.mytbt.Routing.APIs.OsrmAPI.ResponseModels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Luís Henriques for MyTbt.
 * 16-04-2018, 16:34
 */
@SuppressWarnings("UnusedDeclaration")
public class OsrmGeometry {

    @SerializedName("coordinates")
    @Expose
    private List<double[]> coordinates;

    public OsrmGeometry() {
    }

    public List<double[]> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<double[]> coordinates) {
        this.coordinates = coordinates;
    }
}

package com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luís Henriques for MyTbt.
 * 26-12-2017, 12:10
 *
 * Model for Bing API
 * BingRouteResourceSet > BingRouteResource > BingRouteLeg > BingItineraryItem > BingInstruction + BingManeuverPoint + BingWarning
 *                                  > BingRoutePath > BingLine
 */
@SuppressWarnings("UnusedDeclaration")
public class BingLine {

    @SerializedName("coordinates")
    @Expose
    private List<double[]> coordinates;

    public BingLine() {
    }

    /**
     * @return returns a list of geopoints to draw a line on the map
     */
    public List<GeoPoint> getLinePoints(){
        ArrayList<GeoPoint> allPoints = new ArrayList<>();

        if (coordinates != null){ // preventing bugs
            for (double[] coordinate : coordinates) {
                GeoPoint gp = new GeoPoint(coordinate[0], coordinate[1]);
                allPoints.add(gp);
            }
        }
        return allPoints;
    }

    // model's gets and sets
    public List<double[]> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<double[]> coordinates) {
        this.coordinates = coordinates;
    }
}

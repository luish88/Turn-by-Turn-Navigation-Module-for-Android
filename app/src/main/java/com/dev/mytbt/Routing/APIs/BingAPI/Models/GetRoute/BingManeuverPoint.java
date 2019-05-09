package com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.oscim.core.GeoPoint;


/**
 * Created by Luís Henriques for MyTbt.
 * 22-12-2017, 12:23
 *
 * Model for Bing API
 * BingRouteResourceSet > BingRouteResource > BingRouteLeg > BingItineraryItem > BingInstruction + BingManeuverPoint + BingWarning
 *                                  > BingRoutePath > BingLine
 */
@SuppressWarnings("UnusedDeclaration")
public class BingManeuverPoint {

    @SerializedName("coordinates")
    @Expose
    private double[] coordinates;

    public BingManeuverPoint() {
    }

    // direct gets
    public GeoPoint getGeoPoint(){
        return new GeoPoint(coordinates[0], coordinates[1]);
    }

    /**
     *
     * @return returns the point's coordinates with the following structure: latitude,longitude
     */
    public String getPoint(){
        return String.valueOf(coordinates[0]) + "," + String.valueOf(coordinates[1]);
    }

    // model's gets and sets
    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }
}

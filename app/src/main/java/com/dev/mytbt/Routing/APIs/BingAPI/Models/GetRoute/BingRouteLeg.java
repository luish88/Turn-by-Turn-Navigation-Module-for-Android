package com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luís Henriques for MyTbt.
 * 22-12-2017, 12:19
 *
 * Model for Bing API
 * BingRouteResourceSet > BingRouteResource > BingRouteLeg > BingActualEnd > coordinates
 *                                             > BingItineraryItem > BingInstruction + BingManeuverPoint + BingWarning
 *                                             > BingRoutePath > BingLine
 */
@SuppressWarnings("UnusedDeclaration")
public class BingRouteLeg {

    @SerializedName("itineraryItems")
    @Expose
    private BingItineraryItem[] itineraryItems;

    @SerializedName("actualEnd")
    @Expose
    private BingActualEnd actualEnd;

    @SerializedName("routeSubLegs")
    @Expose
    private BingRouteSubLeg[] routeSubLegs;


    public BingRouteLeg() {
    }

    /**
     * @return Returns a list of all GeoPoints in the route
     */
    public List<GeoPoint> getRoutePoints(){
        ArrayList<GeoPoint> allPoints = new ArrayList<>();

        if (itineraryItems != null){ // preventing bugs
            for (BingItineraryItem bingItineraryItem : itineraryItems) {
                allPoints.add(bingItineraryItem.getManeuverPoint().getGeoPoint());
            }
        }
        return allPoints;
    }

    /**
     * @return Returns a string with a list of all points with the following structure: point1Latitude,point1Longitude;point2Latitude,point2Longitude; ...
     */
    public String getAllPointsOnString(){
        StringBuilder allPointsOnString = new StringBuilder();

        if (itineraryItems != null){ // preventing bugs
            for (BingItineraryItem bingItineraryItem : itineraryItems) {
                allPointsOnString.append(bingItineraryItem.getManeuverPoint().getPoint()).append(";");
            }
        }
        allPointsOnString.setLength(allPointsOnString.length()-1); // removing the last ";" from the string
        return allPointsOnString.toString();
    }


    // regular gets and sets
    public BingItineraryItem[] getItineraryItems() {
        return itineraryItems;
    }

    public void setItineraryItems(BingItineraryItem[] itineraryItems) {
        this.itineraryItems = itineraryItems;
    }

    public BingActualEnd getActualEnd() {
        return actualEnd;
    }

    public void setActualEnd(BingActualEnd actualEnd) {
        this.actualEnd = actualEnd;
    }

    public BingRouteSubLeg[] getRouteSubLegs() {
        return routeSubLegs;
    }

    public void setRouteSubLegs(BingRouteSubLeg[] routeSubLegs) {
        this.routeSubLegs = routeSubLegs;
    }
}
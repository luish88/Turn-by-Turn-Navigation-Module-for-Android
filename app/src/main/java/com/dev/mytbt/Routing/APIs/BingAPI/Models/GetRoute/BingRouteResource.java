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
public class BingRouteResource {

    @SerializedName("trafficCongestion")
    @Expose
    private String trafficCongestion;

    @SerializedName("travelDistance")
    @Expose
    private double travelDistance;

    @SerializedName("travelDuration")
    @Expose
    private double travelDuration;

    @SerializedName("travelDurationTraffic")
    @Expose
    private double travelDurationTraffic;

    @SerializedName("routeLegs")
    @Expose
    private BingRouteLeg[] routeLegs;

    @SerializedName("routePath")
    @Expose
    private BingRoutePath routePath;

    public BingRouteResource() {
    }

    public String getTrafficCongestion() {
        return trafficCongestion;
    }

    public void setTrafficCongestion(String trafficCongestion) {
        this.trafficCongestion = trafficCongestion;
    }

    public double getTravelDistance() {
        return travelDistance;
    }

    public void setTravelDistance(double travelDistance) {
        this.travelDistance = travelDistance;
    }

    public double getTravelDuration() {
        return travelDuration;
    }

    public void setTravelDuration(double travelDuration) {
        this.travelDuration = travelDuration;
    }

    public double getTravelDurationTraffic() {
        return travelDurationTraffic;
    }

    public void setTravelDurationTraffic(double travelDurationTraffic) {
        this.travelDurationTraffic = travelDurationTraffic;
    }

    public BingRouteLeg[] getRouteLegs() {
        return routeLegs;
    }

    public void setRouteLegs(BingRouteLeg[] routeLegs) {
        this.routeLegs = routeLegs;
    }

    public BingRoutePath getRoutePath() {
        return routePath;
    }

    public void setRoutePath(BingRoutePath routePath) {
        this.routePath = routePath;
    }
}

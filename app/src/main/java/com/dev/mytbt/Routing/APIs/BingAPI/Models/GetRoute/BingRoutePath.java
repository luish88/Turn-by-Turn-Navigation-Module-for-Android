package com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 26-12-2017, 12:08
 *
 * Model for Bing API
 * BingRouteResourceSet > BingRouteResource > BingRouteLeg > BingItineraryItem > BingInstruction + BingManeuverPoint + BingWarning
 *                                  > BingRoutePath > BingLine
 */
@SuppressWarnings("UnusedDeclaration")
public class BingRoutePath {

    // NOTE: Consider these
    /*
    "trafficCongestion": "None",
    "trafficDataUsed": "None",
    "travelDistance": 2.723,
    "travelDuration": 375,
    "travelDurationTraffic": 383
    **/

    @SerializedName("line")
    @Expose
    private BingLine line;

    public BingRoutePath() {
    }

    public BingLine getLine() {
        return line;
    }

    public void setLine(BingLine line) {
        this.line = line;
    }
}

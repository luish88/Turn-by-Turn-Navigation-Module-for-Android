package com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 22-12-2017, 12:22
 *
 * Model for Bing API
 * BingRouteResourceSet > BingRouteResource > BingRouteLeg > BingItineraryItem > BingInstruction + BingManeuverPoint + BingWarning
 *                                  > BingRoutePath > BingLine
 */
@SuppressWarnings("UnusedDeclaration")
public class BingInstruction {

    @SerializedName("maneuverType")
    @Expose
    private String maneuverType;

    @SerializedName("text")
    @Expose
    private String text;

    public BingInstruction() {
    }

    public String getManeuverType() {
        return maneuverType;
    }

    public void setManeuverType(String maneuverType) {
        this.maneuverType = maneuverType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

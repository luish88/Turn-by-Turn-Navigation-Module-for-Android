package com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 22-12-2017, 12:20
 *
 * Model for Bing API
 * BingRouteResourceSet > BingRouteResource > BingRouteLeg > BingItineraryItem > BingInstruction + BingManeuverPoint + BingWarning
 *                                  > BingRoutePath > BingLine
 */
@SuppressWarnings("UnusedDeclaration")
public class BingItineraryItem {

    @SerializedName("instruction")
    @Expose
    private BingInstruction instruction;

    @SerializedName("maneuverPoint")
    @Expose
    private BingManeuverPoint maneuverPoint;

    @SerializedName("warnings")
    @Expose
    private BingWarning[] warnings;

    @SerializedName("details")
    @Expose
    private BingDetails[] details;

    public BingItineraryItem() {
    }

    public BingInstruction getInstruction() {
        return instruction;
    }

    public void setInstruction(BingInstruction instruction) {
        this.instruction = instruction;
    }

    public BingManeuverPoint getManeuverPoint() {
        return maneuverPoint;
    }

    public void setManeuverPoint(BingManeuverPoint maneuverPoint) {
        this.maneuverPoint = maneuverPoint;
    }

    public BingWarning[] getWarnings() {
        return warnings;
    }

    public void setWarnings(BingWarning[] warnings) {
        this.warnings = warnings;
    }

    public BingDetails[] getDetails() {
        return details;
    }

    public void setDetails(BingDetails[] details) {
        this.details = details;
    }
}

package com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 22-12-2017, 13:30
 *
 * Model for Bing API
 * BingRouteResourceSet > BingRouteResource > BingRouteLeg > BingItineraryItem > BingInstruction + BingManeuverPoint + BingWarning
 *                                  > BingRoutePath > BingLine
 */
@SuppressWarnings("UnusedDeclaration")
public class BingWarning {

    @SerializedName("severity")
    @Expose
    private String severity;

    @SerializedName("warningType")
    @Expose
    private String warningType;

    public BingWarning() {
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getWarningType() {
        return warningType;
    }

    public void setWarningType(String warningType) {
        this.warningType = warningType;
    }
}

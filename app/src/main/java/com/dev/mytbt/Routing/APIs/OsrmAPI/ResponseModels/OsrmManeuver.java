package com.dev.mytbt.Routing.APIs.OsrmAPI.ResponseModels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 16-04-2018, 18:03
 */
@SuppressWarnings("UnusedDeclaration")
public class OsrmManeuver {

    @SerializedName("type")
    @Expose
    private String type;

    @SerializedName("modifier")
    @Expose
    private String modifier;

    @SerializedName("exit")
    @Expose
    private String exit;

    @SerializedName("location")
    @Expose
    private double[] location;

    public OsrmManeuver() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public String getExit() {
        return exit;
    }

    public void setExit(String exit) {
        this.exit = exit;
    }

    public double[] getLocation() {
        return location;
    }

    public void setLocation(double[] location) {
        this.location = location;
    }
}



package com.dev.mytbt.Routing.APIs.OsrmAPI.ResponseModels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 16-04-2018, 16:36
 */
@SuppressWarnings("UnusedDeclaration")
public class OsrmLegs {

    @SerializedName("steps")
    @Expose
    private OsrmStep[] steps;

    public OsrmLegs() {
    }

    public OsrmStep[] getSteps() {
        return steps;
    }

    public void setSteps(OsrmStep[] steps) {
        this.steps = steps;
    }
}

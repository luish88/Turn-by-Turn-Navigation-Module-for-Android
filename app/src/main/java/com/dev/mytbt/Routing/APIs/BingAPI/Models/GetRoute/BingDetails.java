package com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Luís Henriques for MyTbt.
 * 07-03-2018, 10:48
 */
@SuppressWarnings("UnusedDeclaration")
public class BingDetails {

    @SerializedName("names")
    @Expose
    private String[] names;

    public BingDetails() {
    }

    public String[] getNames() {
        return names;
    }

    public void setNames(String[] names) {
        this.names = names;
    }
}

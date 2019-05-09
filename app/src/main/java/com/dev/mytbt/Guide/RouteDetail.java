package com.dev.mytbt.Guide;

import com.dev.mytbt.Routing.RoutePoint;

/**
 * Created by Luís Henriques for MyTbt.
 * 06-03-2018, 13:19
 */
public class RouteDetail {

    private RoutePoint.DirIcon icon;
    private String streetName;
    private double distance;
    private String distanceText = "";
    private String speedLimit;
    private boolean passedBy;

    RouteDetail(RoutePoint.DirIcon icon, String streetName, double distance, String speedLimit, boolean passedBy) {
        this.icon = icon;
        this.streetName = streetName;
        this.distance = distance;
        this.speedLimit = speedLimit;
        this.passedBy = passedBy;
    }

    public void setDistanceText(String distanceText) {
        this.distanceText = distanceText;
    }

    public RoutePoint.DirIcon getIcon() {
        return icon;
    }

    public String getStreetName() {
        return streetName;
    }

    public double getDistance() {
        return distance;
    }

    String getDistanceText() {
        return distanceText;
    }

    String getSpeedLimit() {
        return speedLimit;
    }

    public boolean isPassedBy() {
        return passedBy;
    }
}

package com.dev.mytbt.Guide;

import com.dev.mytbt.Routing.RoutePoint;

import org.oscim.core.GeoPoint;

/**
 * Created by Luís Henriques for MyTbt.
 * 18-11-2018, 21:03
 *
 * This model is responsible for keeping information about a marker on the map
 * It simply stores the position (GeoPoint) and type of marker
 */
public class NavPathMarker {

    private RoutePoint.MarkerType marker;
    private GeoPoint point;

    NavPathMarker(RoutePoint.MarkerType marker, GeoPoint point) {
        this.marker = marker;
        this.point = point;
    }

    public RoutePoint.MarkerType getMarkerType() {
        return marker;
    }

    public GeoPoint getPoint() {
        return point;
    }
}

package com.dev.mytbt.Guide;

import org.oscim.core.GeoPoint;

import java.util.List;

/**
 * Created by Luís Henriques for MyTbt.
 * 16-11-2018, 22:56
 *
 * This model is responsible for keeping information about a path layer to be drawn on the map
 * It simply stores the list of points on the layer and its traffic level
 */
public class NavPathLayer {
    private int trafficLevel;
    private List<GeoPoint> points;

    NavPathLayer(int trafficLevel, List<GeoPoint> points) {
        this.trafficLevel = trafficLevel;
        this.points = points;
    }

    public int getTrafficLevel() {
        return trafficLevel;
    }

    public List<GeoPoint> getPoints() {
        return points;
    }
}

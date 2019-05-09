package com.dev.mytbt.Routing;

import android.util.Log;

import com.dev.mytbt.R;

import org.oscim.core.GeoPoint;

/**
 * Created by Luís Henriques for MyTbt.
 * 12-01-2018, 10:48
 *
 * RoutePoint model to be read by the navigation system
 * Every API's route responses should be translated to RoutePoints
 */
public class RoutePoint {

    private static final String TAG = "tbtRoutePoint";

    private GeoPoint geoPoint;
    private String instruction = "";
    private int trafficLevel = 0; // -1 = Offline, 0 = No Traffic, 1 = Minor, 2 = Moderate, 3 = Serious
    private boolean isWaypoint = false; // is this object a destination (waypoint)?
    public enum DirIcon {UNKNOWN, START, READJUSTED, CONTINUE, DESTINATION, TURN_SLIGHT_LEFT, TURN_SLIGHT_RIGHT, TURN_LEFT, TURN_RIGHT, TURN_SHARP_LEFT, TURN_SHARP_RIGHT, KEEP_LEFT, KEEP_RIGHT, ON_RAMP_LEFT, ON_RAMP_RIGHT, OFF_RAMP_LEFT, OFF_RAMP_RIGHT, ROUNDABOUT, LEAVE_ROUNDABOUT, U_TURN, ROAD_NAME_CHANGE}
    private DirIcon myIcon = DirIcon.UNKNOWN; // default DirIcon
    private int roundaboutExit = 0; // in case this point is a roundabout, this defines the appropriate exit for the user to take
    private String streetName = "";
    private String recommendedSpeed = "";
    // NOTE: START, READJUSTED and DESTINATION markers are set by the NavPath constructor. TRAFFIC and MASS_TRANSIT are set by each NavPath. All others are the adapter's responsibility.
    public enum MarkerType {NONE, START, READJUSTED, DESTINATION, TRAFFIC, MASS_TRANSIT, ACCIDENT, BLOCKED_ROAD, DISABLED_VEHICLE, ROAD_HAZARD, CONSTRUCTION}
    private MarkerType myMarker = MarkerType.NONE;

    // automatically assigned
    private double distanceFromPreviousPoint = 0.0f;

    public RoutePoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    /**
     * returns an icon resource according to the specified icon
     * @param icon the icon
     */
    public static int getInstructionIconResource(DirIcon icon) {
        switch (icon) {
            case START:
                return R.drawable.dir_start;
            case READJUSTED:
                return R.drawable.dir_readjustedpath;
            case CONTINUE:
                return R.drawable.dir_continue;
            case DESTINATION:
                return R.drawable.dir_destination;
            case TURN_SLIGHT_LEFT:
                return R.drawable.dir_turnslightleft;
            case TURN_SLIGHT_RIGHT:
                return R.drawable.dir_turnslightright;
            case TURN_LEFT:
                return R.drawable.dir_turnleft;
            case TURN_RIGHT:
                return R.drawable.dir_turnright;
            case TURN_SHARP_LEFT:
                return R.drawable.dir_turnsharpleft;
            case TURN_SHARP_RIGHT:
                return R.drawable.dir_turnsharpright;
            case KEEP_LEFT:
                return R.drawable.dir_keepleft;
            case KEEP_RIGHT:
                return R.drawable.dir_keepright;
            case ON_RAMP_LEFT:
                return R.drawable.dir_onrampleft;
            case ON_RAMP_RIGHT:
                return R.drawable.dir_onrampright;
            case OFF_RAMP_LEFT:
                return R.drawable.dir_offrampexitleft;
            case OFF_RAMP_RIGHT:
                return R.drawable.dir_offrampexitright;
            case ROUNDABOUT:
                return R.drawable.dir_roundabout;
            case LEAVE_ROUNDABOUT:
                return R.drawable.dir_leaveroundabout;
            case U_TURN:
                return R.drawable.dir_turnback;
            case ROAD_NAME_CHANGE:
                return R.drawable.dir_roadnamechange;
            default: // UNKNOWN and others
                return R.drawable.dir_continue;
        }
    }

    public static int getMarkerIconResource(MarkerType markerType) {
        switch (markerType) {
            case START:
                return R.drawable.mk_start;
            case READJUSTED:
                return R.drawable.mk_readjusted;
            case DESTINATION:
                return R.drawable.mk_destination;
            case TRAFFIC:
                return R.drawable.mk_path_traffic;
            case MASS_TRANSIT:
                return R.drawable.mk_path_mass_transit;
            case ACCIDENT:
                return R.drawable.mk_path_accident;
            case BLOCKED_ROAD:
                return R.drawable.mk_path_blocked;
            case DISABLED_VEHICLE:
                return R.drawable.mk_path_disabled_vehicle;
            case ROAD_HAZARD:
                return R.drawable.mk_path_hazard;
            case CONSTRUCTION:
                return R.drawable.mk_path_construction;
            case NONE:
                Log.e(TAG, "ERROR: trying to retrieve a marker for condition NONE.");
                return 0;
            default: // includes OTHER
                return R.drawable.mk_path_other;
        }
    }


    // gets & sets
    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public int getTrafficLevel() {
        return trafficLevel;
    }

    public void setTrafficLevel(int trafficLevel) {
        this.trafficLevel = trafficLevel;
    }

    public boolean isWaypoint() {
        return isWaypoint;
    }

    public void setWaypoint(boolean waypoint) {
        isWaypoint = waypoint;
    }

    public DirIcon getIcon() {
        return myIcon;
    }

    public void setIcon(DirIcon icon) {
        this.myIcon = icon;
    }

    public int getRoundaboutExit() {
        return roundaboutExit;
    }

    public void setRoundaboutExit(int roundaboutExit) {
        this.roundaboutExit = roundaboutExit;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public double getDistanceFromPreviousPoint() {
        return distanceFromPreviousPoint;
    }

    void setDistanceFromPreviousPoint(double distanceFromPreviousPoint) {
        this.distanceFromPreviousPoint = distanceFromPreviousPoint;
    }

    public String getRecommendedSpeed() {
        return recommendedSpeed;
    }

    public void setRecommendedSpeed(String recommendedSpeed) {
        this.recommendedSpeed = recommendedSpeed;
    }

    public MarkerType getMarker() {
        return myMarker;
    }

    public void setMarker(MarkerType marker) {
        this.myMarker = marker;
    }
}

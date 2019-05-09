package com.dev.mytbt.Routing.APIs;

import com.dev.mytbt.Routing.RoutePoint;

import org.oscim.core.GeoPoint;

import java.util.List;

/**
 * Created by Luís Henriques for MyTbt.
 * 12-01-2018, 10:53
 *
 * API Adapters must implement route calculation for calculateRoute(origin, single destination)
 * Adapters are also responsible for:
 *  - Retrieving data from the respective API
 *  - Translating that data to RoutePoint models
 *  - Setting a GeoPoint with the proper latitude and longitude coordinates for each RoutePoint
 *  - Defining road instructions to be read by the Speech to Text engine for each RoutePoint through setInstruction("instructionText")
 *  - Setting instruction icons for each RoutePoint:
 *      Possible types:
 *          DirIcon {UNKNOWN, START, READJUSTED, CONTINUE, DESTINATION, TURN_SLIGHT_LEFT, TURN_SLIGHT_RIGHT, TURN_LEFT, TURN_RIGHT, TURN_SHARP_LEFT, TURN_SHARP_RIGHT, KEEP_LEFT, KEEP_RIGHT, ROUNDABOUT, LEAVE_ROUNDABOUT, ROAD_NAME_CHANGE}
 *  - Defining traffic levels for each RoutePoint by calling
 *      Possible types:
 *          0 = No Traffic, 1 = Minor, 2 = Moderate, 3 = Serious
 *  - Defining road warnings for each RoutePoint such as transit, accidents, etc... when appropriate through setMarker(MarkerType)
 *      NOTE: START, READJUSTED and DESTINATION markers are set by the NavPath constructor. TRAFFIC and MASS_TRANSIT are set by each NavPath as well.
 *      All others are the adapter's responsibility
 *      Possible types:
 *          MarkerType {NONE, START, READJUSTED, DESTINATION, TRAFFIC, MASS_TRANSIT, ACCIDENT, BLOCKED_ROAD, DISABLED_VEHICLE, ROAD_HAZARD, CONSTRUCTION, OTHER}
 *  - Defining speed limits for each RoutePoint whenever possible with setRecommendedSpeed("speedLimit")
 *  - Defining road names for each RoutePoint whenever possible with setStreetName("streetName")
 *  - Setting the route duration via NavigationData.setRouteDuration(long duration)
 *  - Everything else should be defined by the system
 *
 *  - The result should be a List<RoutePoint> with all included RoutePoints properly completed, and should be returned via the
 *      callbackOnlineRouteReceiver.returnSuccess()
 *  - If the request returns a failure, it should be returned by calling:
 *      returnFailure()
 **/
public abstract class RoutingAPIAdapter {

    private static final String TAG = "tbtAPIAbstractAdapter";

    /**
     * Callback used to process route receiving events
     */
    public interface CallbackOnlineRouteReceiver {
        /**
         * Returns a successful route, calculated by the current API
         * @param routePoints The list of the calculated points as RoutePoints
         */
        void returnSuccess(List<RoutePoint> routePoints);

        /**
         * Processes am online route calculation failure
         * @param origin the intended origin of the route to be calculated
         * @param destination the intended destination
         */
        void returnFailure(GeoPoint origin, GeoPoint destination);
    }

    /**
     * Calculates a route from point A to point B
     */
    public abstract void calcRoute(GeoPoint origin, GeoPoint destination, double bearing, CallbackOnlineRouteReceiver callback);
}

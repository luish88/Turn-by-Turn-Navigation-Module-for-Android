package com.dev.mytbt.Guide;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dev.mytbt.NavConfig;
import com.dev.mytbt.R;
import com.dev.mytbt.Routing.RoutePoint;
import com.dev.mytbt.Routing.Router;
import com.graphhopper.GraphHopper;

import org.oscim.core.GeoPoint;
import org.oscim.core.Point;
import org.oscim.utils.GeoPointUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Luís Henriques for MyTbt.
 * 19-02-2018, 12:23
 *
 * This class handles all navigation and navigation events
 */
public class NavigationController {

    private static final String TAG = "tbtNavigator"; // Debug tag

    private Router router;
    private NavigationGuide navGuide;
    private NavigationData navData;
    private boolean calculatingPath = false;

    private CallbackNavigationEvent triggerNavigationEvent;

    private RoutePoint lastOriginalRoutePoint = null; // the last detected route point from the original route. Must start as null
    private double previousDistanceToPointBehind = 0;

    /**
     * Creates a new NavigationController instance
     * @param hopper a GraphHopper instance, properly loaded and ready to perform route calculations
     * @param guide a NavigationGuide instance, properly loaded and with the text-to-speech system ready
     */
    public NavigationController(GraphHopper hopper, NavigationGuide guide, CallbackNavigationEvent callbackNavigationEvent) {
        navGuide = guide;
        triggerNavigationEvent = callbackNavigationEvent;
        navData = new NavigationData();
        this.router = new Router(hopper);
    }

    /* Models
    -------------------------------------------- */
    /**
     * Model used to return both the user position and
     * the user bearing after an update of the GPS cycle
     */
    public class PositionUpdate {

        GeoPoint userPos;
        float userBearing;
        @Nullable NextInstructionInfo nextInstructionInfo;

        PositionUpdate(GeoPoint userPos, float userBearing, @Nullable NextInstructionInfo nextInstruction) {
            this.userPos = userPos;
            this.userBearing = userBearing;
            this.nextInstructionInfo = nextInstruction;
        }

        public GeoPoint getUserPos() {
            return userPos;
        }

        public float getUserBearing() {
            return userBearing;
        }

        @Nullable
        public NextInstructionInfo getNextInstructionInfo() {
            return nextInstructionInfo;
        }
    }

    /**
     * Model to hold information about the next instruction
     */
    public class NextInstructionInfo {

        private String recommendedSpeed;
        private RoutePoint.DirIcon icon;
        private String streetName;
        private double distance;

        private NextInstructionInfo(String recommendedSpeed, RoutePoint.DirIcon icon, String streetName, double distance) {
            this.recommendedSpeed = recommendedSpeed;
            this.icon = icon;
            this.streetName = streetName;
            this.distance = distance;
        }

        public String getRecommendedSpeed() {
            return recommendedSpeed;
        }

        public RoutePoint.DirIcon getIcon() {
            return icon;
        }

        public String getStreetName() {
            return streetName;
        }

        public double getDistToInstruction() {
            return distance;
        }
    }

    /**
     * Callback for when a route calculation event occurs
     */
    public interface CallbackNavigationEvent {
        void onNewRouteReceived(@NonNull List<NavPathLayer> pathLayers, @NonNull List<NavPathMarker> markerLayers);
        void onNewRouteFailure();

        void onRouteUpdateReceived(@NonNull List<NavPathLayer> pathLayers, @NonNull List<NavPathMarker> markerLayers);
        void onRouteUpdateFailure();

        void onDetourFromRoute();
        void onRouteCompleted();
    }

    /**
     * Updates the navigation events instance
     */
    public void updateEventsTrigger(CallbackNavigationEvent t) {
        triggerNavigationEvent = t;
    }

    /* Navigating a path
    -------------------------------------------- */
    /**
     * Calculates a new route
     * @param to the destination
     */
    public void calculateNewRouteTo(@NonNull GeoPoint to, @NonNull Context c) {

        if (!calculatingPath) {

            cancelCurrentRoute();

            // we declare what happens when we finish calculating the new route
            Router.CallbackRouter triggerNewRoute = new Router.CallbackRouter() {
                @Override
                public void onReceivingRoute(List<RoutePoint> routePoints) { // when we receive a new route
                    if (routePoints != null && !routePoints.isEmpty()) {
                        List<RoutePoint> finalRoutePoints = addPointBehindUser(routePoints); // we artificially add a point behind the user
                        NavPath navPath = new NavPath(finalRoutePoints);
                        navData.setCurrentPath(navPath);

                        Router.PostProcessCallback afterPostProcessing = totalPathDistance -> {
                            calculatingPath = false;
                            navGuide.onUserStartingNewPath(navPath, c);
                            triggerNavigationEvent.onNewRouteReceived(navPath.getPathLayers(), navPath.getPathMarkers());
                        };

                        router.fillMissingPathInfo(navPath.getPathPoints(), navPath.getInstructionPoints(), afterPostProcessing);

                    } else {
                        calculatingPath = false;
                        cancelCurrentRoute();
                        navGuide.onFailingNewRoute(c);
                        triggerNavigationEvent.onNewRouteFailure();
                    }
                }

                @Override
                public void onFailingRoute() { // when we fail to calculate a new route
                    calculatingPath = false;
                    cancelCurrentRoute();
                    navGuide.onFailingNewRoute(c);
                    triggerNavigationEvent.onNewRouteFailure();
                }
            };

            // we ask the router to calculate the new route
            router.calculateRoute(navData.getLastKnownUserPosition(), to, navData.getLastKnownUserBearing(), Router.ROUTE_FULL, triggerNewRoute);
            calculatingPath = true;
        }
    }

    /**
     * Checks the detected user position against th current path.
     * If we have a path, we return the best matching position and bearing on the path
     * If the user is far enough, we readjust the path, and return the detected
     *      position and bearing
     * If there is no path, we simply return back the detected user position
     *      and bearing
     * This function alerts the navigation guide to give instructions
     * @param detectedUserPos the detected user position for this GPS cycle
     * @param  gpsAccuracy the detected GPS accuracy for this GPS cycle
     * @return a model containing both the user's position and bearing
     */
    public PositionUpdate pinpointUser(@NonNull GeoPoint detectedUserPos, float detectedUserBearing, float gpsAccuracy, @NonNull Context c){

        NavPath path = navData.getCurrentPath();

        // if the user is not following a path or if we are calculating a path, we update the data and return the detected position
        if (path == null || calculatingPath || path.getNextInstruction() == null) {
            navData.updateUserPosition(detectedUserPos, detectedUserBearing);
            return new PositionUpdate(detectedUserPos, detectedUserBearing, null);

        } else { // but if the user is following a path, we return the user's position checked against that path

            // the user may walk freely on a 100 m radius around the first point (point 0)
            double freeWalkRadius = 100;

            /* if the the user didn't begin following the path yet, we allow him to move freely
            if the user is still within the freeWalkRadius of point 0 */
            if (path.getLastPassedPointIndex() == 0 && path.getPathPoints().get(0).getGeoPoint().sphericalDistance(detectedUserPos) < freeWalkRadius) {

                // building the next instruction model
                RoutePoint nextInstRP = path.getNextInstruction();
                NextInstructionInfo nextInstruction = new NextInstructionInfo(
                        nextInstRP.getRecommendedSpeed(),
                        nextInstRP.getIcon(),
                        nextInstRP.getStreetName(),
                        calculateDistanceToInstruction(detectedUserPos, nextInstRP, path)
                );

                navGuide.onUserFoundOnPath(path, detectedUserPos, nextInstruction.getDistToInstruction(), c); // we inform the navigation guide that the user was found on the path
                navData.updateUserPosition(detectedUserPos, detectedUserBearing);
                return new PositionUpdate(detectedUserPos, detectedUserBearing, nextInstruction); // and simply return the detected user position and bearing

            } else if (path.getWaypoint().getGeoPoint().sphericalDistance(detectedUserPos) < NavConfig.WARM_RADIUS)  {
                // if the user reached his destination
                triggerNavigationEvent.onRouteCompleted();
                navGuide.onUserReachedDestination(c);
                navData.updateUserPosition(detectedUserPos, detectedUserBearing);

                cancelCurrentRoute();

                return new PositionUpdate(detectedUserPos, detectedUserBearing, null); // and simply return the detected user position and bearing

            } else {
                /* Then we start looking for the user on the path

                We make a check  for the next 50 path points.
                If everything goes well, we'll just perform this calculation once or twice, as the user is following the path
                NOTE: Should we check against coldRadius instead?
                */
                for (int i = path.getLastPassedPointIndex(); i < path.getLastPassedPointIndex() + 50; i ++ ) {

                    // if there is, at least, one more point in our path
                    if (path.getPathPoints().size() > i + 1) {

                        // we try to identify which points in our path are behind and in front of the user
                        RoutePoint possiblePointBehind = path.getPathPoints().get(i);
                        RoutePoint possiblePointInFront = path.getPathPoints().get(i + 1);

                    /* we measure the distance from the point behind the user to the point in front of him (point A -> point B)
                    NOTE: We can't use possiblePointInFront.getDistanceFromPreviousPoint(), since it may return 0 */
                        double distAtoB = possiblePointBehind.getGeoPoint().sphericalDistance(possiblePointInFront.getGeoPoint());

                        // we also retrieve the distance from the point behind the user to the user's position (point A -> user)
                        double distUserToPointBehind = detectedUserPos.sphericalDistance(possiblePointBehind.getGeoPoint());

                        // and the distance from the user to the point in front of him as well (user -> point B)
                        double distUserToPointInFront = detectedUserPos.sphericalDistance(possiblePointBehind.getGeoPoint());

                        // if we pass by the lastOriginalRoutePoint, we reset it
                        if (lastOriginalRoutePointIsSet() && lastOriginalRoutePoint.equals(possiblePointBehind)) {
                            Log.w(TAG, "Passing by last original RoutePoint");
                            resetLastOriginalRoutePoint();
                        }

                    /*if the user is closer to the point behind him than the point in front of him is, and the same is true for the point
                    in front as well, it means the user is between both points */
                        if ((distUserToPointBehind - gpsAccuracy) < distAtoB
                                && (distUserToPointInFront - gpsAccuracy) < distAtoB) { // we also add the gps accuracy

                            // then we retrieve a point between possiblePointBehind and possiblePointInFront that is the closest to the user's position
                            GeoPoint connectionPoint = findConnectionPoint(
                                    possiblePointBehind.getGeoPoint(),
                                    possiblePointInFront.getGeoPoint(),
                                    detectedUserPos);

                            // then we check the distance between the user and this connection point. If the user is close enough, we know the user is on the path
                            if (connectionPoint.sphericalDistance(detectedUserPos) - gpsAccuracy < NavConfig.WARM_RADIUS) {

                            /*We can assume the user is following th path on this point
                            So we update the user's bearing to be the path's bearing */
                                float bearingOnPath = (float) possiblePointBehind.getGeoPoint().bearingTo(possiblePointInFront.getGeoPoint());

                                // We also won't let the user's position move backwards on the path
                                if (path.progressOnPath(possiblePointBehind)) { // to do so, if the point index was updated
                                    previousDistanceToPointBehind = 0; // we reset the distance to the previous point, as it is no longer relevant. After all, we have a new point behind the user
                                }

                                if (distUserToPointBehind > previousDistanceToPointBehind) { // if the user moved forward

                                    // we build the next instruction model
                                    RoutePoint nextInstRP = path.getNextInstruction();
                                    NextInstructionInfo nextInstruction = new NextInstructionInfo(
                                            nextInstRP.getRecommendedSpeed(),
                                            nextInstRP.getIcon(),
                                            nextInstRP.getStreetName(),
                                            calculateDistanceToInstruction(connectionPoint, nextInstRP, path)
                                    );

                                    previousDistanceToPointBehind = distUserToPointBehind; // we update the distance to the point behind
                                    navGuide.onUserFoundOnPath(path, connectionPoint, nextInstruction.getDistToInstruction(), c);
                                    navData.updateUserPosition(connectionPoint, bearingOnPath);
                                    return new PositionUpdate(connectionPoint, bearingOnPath, nextInstruction); // and return the user's position on the path line (the connection point's position)

                                } else {
                                    /* but if the user didn't move forward, we want to return a point on the same position as before. This way, if
                                    the user moves backwards, the marker won't move backwards as well. It stays on the same position as before */
                                    float bearing = (float) possiblePointBehind.getGeoPoint().bearingTo(possiblePointInFront.getGeoPoint());
                                    GeoPoint lastUserPosOnPath = possiblePointBehind.getGeoPoint().destinationPoint(previousDistanceToPointBehind, bearing);

                                    // we build the next instruction model, calculating the distance from that same position as before
                                    RoutePoint nextInstRP = path.getNextInstruction();
                                    NextInstructionInfo nextInstruction = new NextInstructionInfo(
                                            nextInstRP.getRecommendedSpeed(),
                                            nextInstRP.getIcon(),
                                            nextInstRP.getStreetName(),
                                            calculateDistanceToInstruction(lastUserPosOnPath, nextInstRP, path)
                                    );

                                    navGuide.onUserFoundOnPath(path, lastUserPosOnPath, nextInstruction.getDistToInstruction(), c);
                                    navData.updateUserPosition(lastUserPosOnPath, bearingOnPath);
                                    return new PositionUpdate(lastUserPosOnPath, bearingOnPath, nextInstruction);
                                }
                            }
                        }

                    }
                }
                // if we arrive at this point without returning a valid user position on our path, we adjust the route
                adjustPath(path, detectedUserPos, c);
            }

            // if we reach this point, it means we didn't find the user on the path
            navGuide.onUserNotFoundOnPath();
            triggerNavigationEvent.onDetourFromRoute();
            navData.updateUserPosition(detectedUserPos, detectedUserBearing);
            return new PositionUpdate(detectedUserPos, detectedUserBearing, null); // and simply return the detected user position and bearing
        }
    }

    /**
     * Finds a point on the line between pointA and pointB that is the closest to the specified position
     * @param pointA the starting point of a line
     * @param pointB the end point of a line
     * @param position a position that is, supposedly, somewhere between point A and point B
     * @return a point on the line between A and B that is the closest to the given position
     */
    private GeoPoint findConnectionPoint(@NonNull GeoPoint pointA, @NonNull GeoPoint pointB, @NonNull GeoPoint position){

        double behindX = pointA.getLongitude();
        double behindY = pointA.getLatitude();
        double inFrontX = pointB.getLongitude();
        double inFrontY = pointB.getLatitude();
        double posX = position.getLongitude();
        double posY = position.getLatitude();

        // so we calculate the new point in our straight road segment that is the closest to the user
        Point p = GeoPointUtils.nearestSegmentPoint(behindX, behindY, inFrontX, inFrontY, posX, posY);

        // and we translate it to a GeoPoint
        return new GeoPoint(p.getY(), p.getX());
    }

    public void cancelCurrentRoute() {
        previousDistanceToPointBehind = 0;
        resetLastOriginalRoutePoint();
        navData.setCurrentPath(null);
    }

    /* Adjusting a path
    -------------------------------------------- */

    /* To decide whether we try to readjust our path by performing a full path recalculation (online, if possible)
        to the waypoint or, on the other hand, we try to find a point mid-path and perform an exclusively offline
        calculation to it, we'll take a "green zone" approach.
        This means that, when we don't find the user following our path, and if we don't have a connection point assigned
        (named lastOriginalRoutePoint), we'll try to find the point that is the furthest one away within a specified radius
        (greenZoneMidPoint). If we find it, we'll assign it as being the lastOriginalRoutePoint, and calculate an
        offline path to it.
        Then, if once we do have a connection point (lastOriginalRoutePoint != null) and the user is not following the path (again):

         - if the lastOriginalRoutePoint is within the green zone (GREEN_ZONE_CLOSE_EDGE to GREEN_ZONE_FAR_EDGE), we'll simply delete
        our path up to it and perform an offline path calculation to lastOriginalRoutePoint.

         - if the lastOriginalRoutePoint is too close (distance < GREEN_ZONE_CLOSE_EDGE), we'll simply delete it and try to find
         another one to recalculate a path to. Just like before, this new point should be the furthest away within
         greenZoneMidPoint. It is now the new lastOriginalRoutePoint.
                NOTE: We must guarantee that this new point is never behind the old lastOriginalRoutePoint, so we'll start
                our search from the old lastOriginalRoutePoint's index.

         - if the user is too far away from the lastOriginalRoutePoint (distance > GREEN_ZONE_FAR_EDGE), we'll also try to find a
         new connectionPoint in our path, and assign it to be the new lastOriginalRoutePoint. If we are unable to find it, we'll
         perform a full path calculation from scratch.
                NOTE: Just like before, we must guarantee that this new point is never behind the old lastOriginalRoutePoint,
                so we'll start our search from the old lastOriginalRoutePoint's index.

         - if, for any reason we are unable to find any of these points, we'll perform a full path recalculation from scratch.

        NOTES:
            - At any time, if the user is too close to the waypoint (distance < GREEN_ZONE_FAR_EDGE), we simply return it as being
            the lastOriginalRoutePoint.
            - At any time, if the user passes by the lastOriginalRoutePoint while following the path, we simply "delete it" (= null).
            - Similarly, if the user starts a new path or resets navigation for any reason, we delete the lastOriginalRoutePoint
            as well.
            - Why don't we simply perform offline path calculation up to the waypoint? Because we want to keep as much traffic
            information as possible, which gets deleted when we perform offline path calculations.*/


    /**
     * Reroutes the given path from the user's position to its destination
     * @param path the pat to be rerouted
     * @param userPosition the detected user position on this GPS cycle
     */
    private void adjustPath(@NonNull NavPath path, @NonNull GeoPoint userPosition, @NonNull Context c){
        // if we couldn't return a valid user position on the path, we adjust the current path
        Log.w(TAG, "--> User deviated from path. Adjusting route. <--");

        // if we don't have a router or a path, we can't navigate
        if (router == null) {
            Log.e(TAG, "ERROR: adjustPath() is missing the Router instance");
            triggerNavigationEvent.onRouteUpdateFailure();
            return;
        }

        // finding the lastOriginalRoutePoint (can be the same one as before)
        lastOriginalRoutePoint = findEdgePointOnPath(path, userPosition);
        if (lastOriginalRoutePointIsSet()) {

            // NOTE: Debug markers. Use for debug
            /* userPos = NavigationData.getUserPosition(context);
            MapMaster.getTempSingleton().drawDebugMarker(lastOriginalRoutePoint.getGeoPoint(), "red");
            double bearing = userPos.bearingTo(lastOriginalRoutePoint.getGeoPoint());
            GeoPoint greenZoneClosePoint = userPos.destinationPoint(GREEN_ZONE_CLOSE_EDGE, (float) bearing);
            GeoPoint greenZoneFarPoint = userPos.destinationPoint(GREEN_ZONE_FAR_EDGE, (float) bearing);
            MapMaster.getTempSingleton().drawDebugMarker(greenZoneClosePoint, "lalala");
            MapMaster.getTempSingleton().drawDebugMarker(greenZoneFarPoint, "blue");*/

            renewPathUpTo(lastOriginalRoutePoint, userPosition, path, c); // if we find it, we calculate a path to it
        } else {
            calculatePathFromScratch(path, userPosition, c);
        }
    }

    /**
     * Finds the last valid point at the edge of the cold radius
     * @param path the path
     * @return Returns a valid point at the edge of the cold radius
     */
    @Nullable
    private RoutePoint findEdgePointOnPath(@NonNull NavPath path, @NonNull GeoPoint userPosition){

        // first of all, if we are too close to the waypoint, we simply return it
        if (userPosition.sphericalDistance(path.getWaypoint().getGeoPoint()) < NavConfig.GREEN_ZONE_FAR_EDGE) {
            Log.w(TAG, "--------> User is too close to the path's waypoint. Calculating an offline path to it.");
            return path.getWaypoint();
        }

        // if lastOriginalRoutePoint  is within the green zone, we return it
        if (lastOriginalRoutePointIsSet()) {
            double distanceToLastOriginalRP = userPosition.sphericalDistance(lastOriginalRoutePoint.getGeoPoint());

            if (distanceToLastOriginalRP > NavConfig.GREEN_ZONE_CLOSE_EDGE
                    && distanceToLastOriginalRP < NavConfig.GREEN_ZONE_FAR_EDGE) {
                Log.w(TAG, "--------> lastOriginalRoutePoint is within the green zone. Reusing it.");
                return lastOriginalRoutePoint;
            }

            Log.w(TAG, "--------> lastOriginalRoutePoint is NOT within the green zone. Trying to find a new one.");

        } else {
            Log.w(TAG, "--------> lastOriginalRoutePoint was null. Trying to find a new one.");
        }

        // else, we'll try to find a valid connection point in our path (a new lastOriginalRoutePoint)

        List<RoutePoint> pathPoints = path.getPathPoints();

        int beginSearchIndex;

        // if we have a valid lastOriginalRoutePoint
        if (lastOriginalRoutePointIsSet() // it exists
                && pathPoints.contains(lastOriginalRoutePoint) // it is in our path
                && pathPoints.indexOf(lastOriginalRoutePoint) > path.getLastPassedPointIndex()) { // and it has a higher index than the last point passed by the user
            beginSearchIndex = pathPoints.indexOf(lastOriginalRoutePoint); // we begin our search from its index

        } else { // if we don't have a valid lastOriginalRoutePoint
            beginSearchIndex = path.getLastPassedPointIndex(); // we begin searching from the index of the last point we passed by
        }

        // the point we are going to return
        RoutePoint edgePoint = null;

        // preventing bugs
        if (beginSearchIndex >= pathPoints.size()) {
            Log.e(TAG, "ERROR: beginSearchIndex >= pathPoints.size(). This should NEVER happen. Beginning search from scratch.");
            beginSearchIndex = 0;
        }

        Log.w(TAG, "--------> Looking for a new connection point from index " + beginSearchIndex);

        for (int i = beginSearchIndex; i < pathPoints.size(); i ++) {

            // the point behind the user is ignored. We want to begin our search from the point in front of him
            if (i == path.getLastPassedPointIndex() || i == 0) {
                continue; // we do this just to simplify. It would be more complicated to check pathPoints.size() against beginSearchIndex + 1
            }

            RoutePoint rp = path.getPathPoints().get(i);

            // if we reach the waypoint, we stop looking
            if (rp.isWaypoint()) {
                Log.e(TAG, "--------> ERROR: Looking for a new connection point reached the waypoint. How is this possible? Calculating an offline path to it.");
                edgePoint = rp;
                break;
            }

            // if we are looking too far away already, we simply stop looking
            if (userPosition.sphericalDistance(rp.getGeoPoint()) > NavConfig.GREEN_ZONE_FAR_EDGE) {
                Log.w(TAG, "--------> Looking for a new connection point reached the edge at index: " + i);
                break;
            }

            // if the point is within the green zone (closeEdge to midPoint)
            if (userPosition.sphericalDistance(rp.getGeoPoint()) > NavConfig.GREEN_ZONE_CLOSE_EDGE
                    && userPosition.sphericalDistance(rp.getGeoPoint()) < NavConfig.GREEN_ZONE_FAR_EDGE) {
                edgePoint = rp; // we'll assign it as being our best option so far

            } else if (edgePoint != null) { // if we leave the green zone after finding a point
                break; // we stop searching. We already found a valid point
            }
        }

        if (edgePoint != null) {
            Log.w(TAG, "--------> New connection point found with index: " + pathPoints.indexOf(edgePoint) + ". Calculating an offline path to it.");
        } else {
            Log.w(TAG, "--------> No valid connection point was found. Calculating the full route from scratch.");
        }

        return edgePoint;
    }

    /**
     * Checks if a last original route point is set
     * @return whether or not we have a registered route point from
     * the original, non readjusted path
     */
    private boolean lastOriginalRoutePointIsSet() {
        return lastOriginalRoutePoint != null;
    }

    private void resetLastOriginalRoutePoint() {
        lastOriginalRoutePoint = null;
    }

    /**
     * Calculates a path up to the specified point
     * NOTE: This process uses ONLY offline path calculation
     * @param connectionPoint the specified path point
     * @param userPosition The user's position detected on this GPS cycle
     */
    private void renewPathUpTo(RoutePoint connectionPoint, @NonNull GeoPoint userPosition, @NonNull NavPath path, @NonNull Context c) {

        path.discardPointsUpTo(connectionPoint);

        // if the lastOriginalRoutePoint was discarded from the path
        if (lastOriginalRoutePointIsSet() && !path.getPathPoints().contains(lastOriginalRoutePoint)) {
            Log.w(TAG, "------------------> lastOriginalRoutePoint was discarded from the path. Resetting it.");
            resetLastOriginalRoutePoint(); // we reset it so we can set it back again
        }

        // we declare what happens when we finish calculating the adjustment
        Router.CallbackRouter triggerRenewPathUpTo = new Router.CallbackRouter() {
            @Override
            public void onReceivingRoute(List<RoutePoint> routePoints) { // when receive the route adjustment
                if (routePoints != null && !routePoints.isEmpty()) {

                    Router.PostProcessCallback afterPostProcessing = totalPathDistance -> {
                        calculatingPath = false;
                        navGuide.onUserReadjustedPath(c);
                        triggerNavigationEvent.onRouteUpdateReceived(path.getPathLayers(), path.getPathMarkers());
                    };

                    routePoints.remove(routePoints.size()-1); // removing the last point of the received path
                    List<RoutePoint> finalRoutePoints = addPointBehindUser(routePoints); // we artificially add a point behind the user
                    path.addPoints(finalRoutePoints); // we add the received points on our path
                    router.fillMissingPathInfo(path.getPathPoints(), path.getInstructionPoints(), afterPostProcessing);

                } else {
                    calculatingPath = false;
                    cancelCurrentRoute();
                    navGuide.onFailingRouteUpdate(c);
                    triggerNavigationEvent.onRouteUpdateFailure();
                }
            }

            @Override
            public void onFailingRoute() { // when we fail to calculate the route adjustment
                calculatingPath = false;
                cancelCurrentRoute();
                navGuide.onFailingRouteUpdate(c);
                triggerNavigationEvent.onRouteUpdateFailure();
            }
        };

        // we calculate a path up to the found edge point (which is the lastOriginalRoutePoint)
        router.calculateRoute(userPosition, connectionPoint.getGeoPoint(), getUserBearing(), Router.ROUTE_CONNECTOR_PATH, triggerRenewPathUpTo );
        calculatingPath = true;
    }

    /**
     * Fully recalculates the given path from the user's position up to its waypoint
     * NOTE: This process tries to retrieve an online route first
     * @param path The path to be rerouted
     * @param userPosition The user's position detected on this GPS cycle
     */
    private void calculatePathFromScratch(@NonNull NavPath path, @NonNull GeoPoint userPosition, @NonNull Context c){
        // we delete all the path points up to the waypoint
        RoutePoint pathWaypoint = path.getWaypoint();
        path.discardPointsUpTo(pathWaypoint);

        // we declare what happens when we finish calculating the adjustment
        Router.CallbackRouter triggerPathFromScratch = new Router.CallbackRouter() {
            @Override
            public void onReceivingRoute(List<RoutePoint> routePoints) { // when receive the route adjustment
                if (routePoints != null && !routePoints.isEmpty()) {

                    Router.PostProcessCallback afterPostProcessing = totalPathDistance -> {
                        calculatingPath = false;
                        navGuide.onUserReadjustedPath(c);
                        triggerNavigationEvent.onRouteUpdateReceived(path.getPathLayers(), path.getPathMarkers());
                    };

                    routePoints.remove(routePoints.size()-1); // removing the last point of the received path
                    List<RoutePoint> finalRoutePoints = addPointBehindUser(routePoints); // we artificially add a point behind the user
                    path.addPoints(finalRoutePoints); // we add the received points on our path
                    router.fillMissingPathInfo(path.getPathPoints(), path.getInstructionPoints(), afterPostProcessing);

                } else {
                    calculatingPath = false;
                    cancelCurrentRoute();
                    navGuide.onFailingRouteUpdate(c);
                    triggerNavigationEvent.onRouteUpdateFailure();
                }
            }

            @Override
            public void onFailingRoute() { // when we fail to calculate the route adjustment
                calculatingPath = false;
                cancelCurrentRoute();
                navGuide.onFailingRouteUpdate(c);
                triggerNavigationEvent.onRouteUpdateFailure();
            }
        };

        // we recalculate the route from the user's position to the remaining point in our path (the waypoint)
        router.calculateRoute(userPosition, pathWaypoint.getGeoPoint(), getUserBearing(), Router.ROUTE_FROM_SCRATCH, triggerPathFromScratch);
        calculatingPath = true;
        resetLastOriginalRoutePoint(); // we also reset the last original RoutePoint, since it is not valid anymore
    }

    /* Route processing
    -------------------------------------------------- */
    /**
     * Artificially adds a point behind the user, so it doesn't keep calculating a new route
     * (the user position changes while we are calculating a route, so the first path points
     * do not match when we receive the path). Also removes all obsolete points that the user
     * passed in the meantime.
     * @param points the path's points
     * @return the path's points, with an added point behind the user (if needed)
     */
    private List<RoutePoint> addPointBehindUser(List<RoutePoint> points) {

        // preventing bugs
        if (points.size() >= 2) {

            // first we retrieve the point that is the closest to the user
            RoutePoint closestPoint = points.get(0);
            GeoPoint userPos = navData.getLastKnownUserPosition();
            double closestDistance = Double.MAX_VALUE;

            // we'll also build a list of all points that are behind the user, which we will delete
            List<RoutePoint> obsoletePoints = new ArrayList<>();

            for (RoutePoint rp : points) {
                double distToUser = rp.getGeoPoint().sphericalDistance(userPos);

                if (distToUser < closestDistance) {
                    closestDistance = distToUser;
                    closestPoint = rp;
                    obsoletePoints.add(rp); // if the closest point is in front of the user, we'll remove it later

                } else {

                    // we break the cycle once the distance starts increasing
                    break;
                }
            }

            /* now we check if the user is in front or behind the closest point. To do so, we check the user's
            distance to the next point, and compare it with this point's distance to the next point */
            GeoPoint pointInFront;
            int rpIndex = points.indexOf(closestPoint);

            // if there is a next point
            if (points.size() > rpIndex + 1) {

                // if the user is further away from the next point than the closest point is
                if (userPos.sphericalDistance(points.get(rpIndex + 1).getGeoPoint()) > closestPoint.getGeoPoint().sphericalDistance(points.get(rpIndex + 1).getGeoPoint())) {
                    // it means the user is behind the closest point
                    pointInFront = closestPoint.getGeoPoint();
                    obsoletePoints.remove(closestPoint); // we remove this point from the list of obsolete points. It is not obsolete after all.

                } else {
                    pointInFront = points.get(rpIndex + 1).getGeoPoint();

                }

            } else {
                // just a safety switch, so pointInFront is never null
                pointInFront = closestPoint.getGeoPoint();
                obsoletePoints.remove(closestPoint);
            }

            // we check the user's direction to the point in front
            float bearingFromFirstPoint = (float) pointInFront.bearingTo(userPos);

            // and create a new point in the opposite direction
            GeoPoint newGeoPoint = userPos.destinationPoint(NavConfig.WARM_RADIUS / 2, bearingFromFirstPoint);
            RoutePoint newRP = new RoutePoint(newGeoPoint);

            // we also remove all points behind the user
            points.removeAll(obsoletePoints);

            // and repopulate the new RoutePoint with the elements from the next point
            newRP.setRecommendedSpeed(points.get(0).getRecommendedSpeed());
            newRP.setStreetName(points.get(0).getStreetName());

            // then we finally add the new point behind the user
            points.add(0, newRP);

        } else {
            Log.e(TAG, "ERROR: addPointBehindUser() is working with an empty list of points ");
        }

        return points;
    }

    /**
     * Calculates the distance from the given position to the specified instruction point
     * @param fromPoint the point from which we will calculate the distance to the next instruction. Usually the user's position
     * @param instructionPoint the specified instruction point
     * @return the distance to the instruction, in meters or -1, in case the calculation can't be performed yet (we might still be post-processing this route)
     */
    private double calculateDistanceToInstruction(@NonNull GeoPoint fromPoint, @NonNull RoutePoint instructionPoint, @NonNull NavPath path){

        double distToInstruction = 0;

        List<RoutePoint> pathPoints = path.getPathPoints();

        // for every point in our path from the point in front of the user up to the instruction point
        int instructionPointIndex = pathPoints.indexOf(instructionPoint);

        // Debug
        if (instructionPointIndex <= path.getLastPassedPointIndex()) {
            Log.e(TAG, "ERROR: calculateDistanceToInstruction() -> instructionPointIndex <= path.getLastPassedPointIndex())! This should never happen!" );
            return 0;
        }

        // for every point in our path from the point in front of the user
        for (int i = path.getLastPassedPointIndex() + 1; i < pathPoints.size() ; i++) {

            RoutePoint currentPoint = pathPoints.get(i);

            if (i == path.getLastPassedPointIndex() + 1) { // if this is the point in front of the user
                distToInstruction = fromPoint.sphericalDistance(currentPoint.getGeoPoint());
            } else {
                distToInstruction += currentPoint.getDistanceFromPreviousPoint();
            }

            // once we find our instruction point, we stop the calculations and return the distance
            if (currentPoint.equals(instructionPoint)) {
                break;
            }
        }
        return  distToInstruction;
    }

    /* Navigation lifecycle
    -------------------------------------------- */
    public void pauseNavigation() {
        navGuide.closeTextToSpeechService();
    }

    public void resumeNavigation(Context c) {
        navGuide.resumeAndPlay(c);
    }

    public void closeNavigation() {
        router.closeHopper();
    } // should be called on application close, but onDestroy() and Activity's isFinishing() are called onPause()

    /* Gets
    -------------------------------------------- */
    public boolean hasPath() {
        return navData.hasPath();
    }

    public boolean isCalculatingPath() {
        return calculatingPath;
    }

    public GeoPoint getUserPosition() {
        return navData.getLastKnownUserPosition();
    }

    public float getUserBearing() {
        return navData.getLastKnownUserBearing();
    }

    @NonNull
    public List<RouteDetail> getRouteDetails(Context c) {
        List<RouteDetail> details = new ArrayList<>();
        NavPath path = navData.getCurrentPath();

        if (path != null) {
            List<RoutePoint> passedInstructions = path.getPassedInstructionPoints();

            // building the list of passed instructions
            for (RoutePoint instruction : passedInstructions) {

                if (instruction.getIcon().equals(RoutePoint.DirIcon.START)) { // if it is the starting point

                    if (instruction.getStreetName().isEmpty()) { // if there is no street name, we simply say "departure"
                        try {
                            instruction.setStreetName(c.getString(R.string.route_details_start));
                        } catch (Exception e) {
                            instruction.setStreetName("");
                        }
                    } else {
                        // but if we have a street name, we build the string "departure from (street name)"
                        String streetName = instruction.getStreetName();
                        try {
                            if (!streetName.contains(c.getString(R.string.route_details_start))) {
                                instruction.setStreetName(
                                        c.getString(R.string.route_details_start)
                                                .concat(" ")
                                                .concat(c.getString(R.string.route_details_start_suffix))
                                                .concat(" ")
                                                .concat(streetName));
                            }
                        } catch (Exception e) {
                            instruction.setStreetName("");
                        }
                    }
                }

                RouteDetail routeDetail = new RouteDetail(
                        instruction.getIcon(),
                        instruction.getStreetName(),
                        0,
                        instruction.getRecommendedSpeed(),
                        true);

                details.add(routeDetail);
            }

            // to calculate each instruction's distance to the user, we need to iterate through  all remaining RoutePoints from the current path
            double dist = 0.0;
            if (path.getLastPassedPointIndex() + 1 < path.getPathPoints().size()) {
                for (int i = path.getLastPassedPointIndex() + 1; i < path.getPathPoints().size() ; i ++) { // we start the loop from the point in front of the user

                    RoutePoint rp = path.getPathPoints().get(i);

                    if (i == path.getLastPassedPointIndex() + 1) { // for the first point, we simply calculate its distance to the user
                        dist = navData.getLastKnownUserPosition().sphericalDistance(rp.getGeoPoint());
                    } else {
                        // for any other point, we iterate its distance to the previous point
                        dist += rp.getDistanceFromPreviousPoint();
                    }

                    // if it is an instruction
                    if (path.getInstructionPoints().contains(rp)) {
                        // once we find an instruction point, we create an instruction item based on its data
                        RouteDetail routeDetail = new RouteDetail(
                                rp.getIcon(),
                                rp.getStreetName(),
                                dist,
                                rp.getRecommendedSpeed(),
                                false);
                        details.add(routeDetail);
                    }
                }
            }
            Collections.reverse(details); // reversing the list, so past points stay on the bottom*/
        }
        return details;
    }

    @NonNull
    public String getDestinationName() {
        if (navData.getCurrentPath() != null) {
            return navData.getCurrentPath().getDestinationName();
        } else {
            return "";
        }
    }

    /**
     * Retrieves the distance to the destination relative to the whole path
     * @return the distance to the current path's waypoint
     */
    public double getDistanceToWaypoint() {
        double dist = 0;
        NavPath path = navData.getCurrentPath();
        if (path != null) {
            dist = calculateDistanceToInstruction(navData.getLastKnownUserPosition(), path.getWaypoint(), path);
        }
        return dist;
    }
}
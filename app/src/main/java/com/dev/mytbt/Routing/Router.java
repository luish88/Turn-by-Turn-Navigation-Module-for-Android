package com.dev.mytbt.Routing;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.dev.mytbt.NavConfig;
import com.dev.mytbt.Routing.APIs.RoutingAPIAdapter;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Translation;
import com.graphhopper.util.TranslationMap;
import com.graphhopper.util.details.PathDetail;
import com.graphhopper.util.shapes.GHPoint;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Luís Henriques for MyTbt.
 * 15-01-2018, 12:19
 *
 * Whenever an online routing request fails, this class
 * should perform an offline route calculation with
 * the Graphopper library
 */
public class Router {

    private static final String TAG = "tbtRouter";

    private RoutingAPIAdapter api;
    private static GraphHopper hopper;
    private boolean calculatingPath = false;

    // Route calculation types
    public static final int ROUTE_FULL = 0; // A full route
    public static final int ROUTE_FROM_SCRATCH = 1; // A full recalculation of the current path
    public static final int ROUTE_CONNECTOR_PATH = 2; // A route that will be attached to another route, for short path adjustments


    /* Instances & Lifecycle
    -------------------------------------------------- */
    /**
     * This class is responsible for handling all routing
     * @param hop A GraphHopper instance, properly loaded and prepared
     */
    public Router(GraphHopper hop) {
        hopper = hop;
        api = NavConfig.getRoutingAPIAdapter();
    }

    /**
     * This class is responsible for returning receiving routes
     */
    public interface CallbackRouter {
        void onReceivingRoute(List<RoutePoint> routePoints);
        void onFailingRoute();
    }

    /**
     * Closes the GraphHopper instance
     */
    public void closeHopper(){
        if (hopper != null) {
            hopper.close();
        }
        hopper = null;
        System.gc();
    }

    /* Routing
    -------------------------------------------------- */
    /**
     * Asks the API to calculate a route from point A to point B
     * @param routeMode ROUTE_FULL is considered a new route. As such, it overrides the current route;
     *                  ROUTE_FROM_SCRATCH recalculates the current path, but maintains its identity and destination point;
     *                  ROUTE_CONNECTOR_PATH will calculate a single path that is supposed to connect to the current path midway.
     * @param callbackToNavigator the callback to handle receiving the calculated routes on the Navigation Controller
     */
    public void calculateRoute(@NonNull GeoPoint origin, @NonNull GeoPoint destination, double userBearing, int routeMode, @NonNull CallbackRouter callbackToNavigator) {

        if (calculatingPath) {
            Log.e(TAG, "ERROR: Router wants to calculate a path while already calculating one. Returning.");
            return;
        }

        calculatingPath = true;

        // if we just want to adjust our current route, we instantly perform an offline calculation
        if (routeMode == ROUTE_CONNECTOR_PATH) { // NOTE: OFFLINE CALCULATION ONLY

            CallbackOfflineRouter triggerOfflineRoute = new CallbackOfflineRouter() {
                @Override
                public void returnSuccess(List<RoutePoint> offlineRPs) {
                    calculatingPath = false;
                    callbackToNavigator.onReceivingRoute(offlineRPs);
                }

                @Override
                public void returnFailure() {
                    calculatingPath = false;
                    callbackToNavigator.onFailingRoute();
                }
            };
            calculateOfflineRoute(origin, destination, userBearing, triggerOfflineRoute);

        }else if (routeMode == ROUTE_FULL || routeMode == ROUTE_FROM_SCRATCH) {
            /* but if we want a new route or if we want to calculate the current route from scratch, we go for an online calculation first.
            To do so, we prepare the online route calculation callback for when we get the result */
            RoutingAPIAdapter.CallbackOnlineRouteReceiver triggerReceiveOnlineRoute = new RoutingAPIAdapter.CallbackOnlineRouteReceiver() {
                @Override
                public void returnSuccess(List<RoutePoint> routePoints) {

                    // if something is wrong with our path, we go for an offline calculation
                    if (routePoints == null || routePoints.isEmpty() || routePoints.size() < 2) {
                        CallbackOfflineRouter triggerOfflineRoute = new CallbackOfflineRouter() {
                            @Override
                            public void returnSuccess(List<RoutePoint> offlineRPs) {
                                calculatingPath = false;
                                callbackToNavigator.onReceivingRoute(offlineRPs);
                            }

                            @Override
                            public void returnFailure() {
                                calculatingPath = false;
                                callbackToNavigator.onFailingRoute();
                            }
                        };
                        calculateOfflineRoute(origin, destination, userBearing, triggerOfflineRoute);

                    } else {
                        // but if everything is fine with our new path
                        calculatingPath = false; // we are not calculating a path anymore
                        callbackToNavigator.onReceivingRoute(routePoints);
                    }
                }

                @Override
                public void returnFailure(GeoPoint origin, GeoPoint destination) {
                    // But when we fail to calculate an online route, we try to do so offline
                    CallbackOfflineRouter triggerOfflineRoute = new CallbackOfflineRouter() {
                        @Override
                        public void returnSuccess(List<RoutePoint> offlineRPs) {
                            calculatingPath = false;
                            callbackToNavigator.onReceivingRoute(offlineRPs);
                        }

                        @Override
                        public void returnFailure() {
                            calculatingPath = false;
                            callbackToNavigator.onFailingRoute();
                        }
                    };
                    calculateOfflineRoute(origin, destination, userBearing, triggerOfflineRoute);
                }
            };
            api.calcRoute(origin, destination, userBearing, triggerReceiveOnlineRoute);

        } else {
            Log.e(TAG, "ERROR: calculateRoute() received an unknown route mode.");
        }
    }

    /* Offline routing
    -------------------------------------------------- */
    private interface CallbackOfflineRouter {
        void returnSuccess(List<RoutePoint> routePoints);
        void returnFailure();
    }

    /**
     * Calculates a route using the GraphHopper library
     */
    private void calculateOfflineRoute(@NonNull GeoPoint origin, @NonNull GeoPoint destination, double userBearing, @NonNull CallbackOfflineRouter callback) {
        // Facilitating code legibility
        CalculateOfflinePathTask offlineRouteTask = new CalculateOfflinePathTask(hopper, origin, destination, userBearing, callback);
        offlineRouteTask.execute();
    }

    /**
     * This AsyncTask calculates a PATH_FROM_SCRATCH from point A to point B or between waypoints (depending on the data received)
     */
    private static class CalculateOfflinePathTask extends AsyncTask<Void, Void, List<RoutePoint>> {

        CallbackOfflineRouter triggerOfflineRouteEvent;
        GraphHopper gHopper;
        GeoPoint origin;
        GeoPoint destination;
        double heading;

        CalculateOfflinePathTask(@NonNull GraphHopper hopper, @NonNull GeoPoint origin, @NonNull GeoPoint destination, double heading, @NonNull CallbackOfflineRouter receiver) {
            this.triggerOfflineRouteEvent = receiver;
            this.gHopper = hopper;
            this.origin = origin;
            this.destination = destination;
            this.heading = heading;
        }

        protected List<RoutePoint> doInBackground(Void... params) {

            List<RoutePoint> routePoints = new ArrayList<>(); // initializing the final list of RoutePoints
            PathWrapper offlineRoute; // the expected resulting offline route

            try {
                // we set up the new request
                GHRequest req = new GHRequest().setAlgorithm(Parameters.Algorithms.DIJKSTRA_BI); // defining the algorithm
                req.getHints().put(Parameters.Routing.INSTRUCTIONS, "true"); // we ask for instructions
                req.getHints().put(Parameters.CH.DISABLE, "true"); // we enable path heading by disabling CH

                // we ask for a couple of details as well
                req.setPathDetails(Arrays.asList("average_speed", "street_name"));


                GHPoint ghFrom = new GHPoint(origin.getLatitude(), origin.getLongitude());
                GHPoint ghTo = new GHPoint(destination.getLatitude(), destination.getLongitude());

                req.addPoint(ghFrom, heading); // origin point has a heading
                req.addPoint(ghTo);


                Log.e(TAG, "gHopper == null?: " + (gHopper == null) );


                // Now we retrieve the best result
                GHResponse response = gHopper.route(req);
                offlineRoute = response.getBest();

                // we set up the language for the turn by turn instructions
                TranslationMap translationMap = new TranslationMap().doImport();
                Translation language = translationMap.getWithFallBack(NavConfig.getLanguage());

                List<Instruction> instructionList = offlineRoute.getInstructions().subList(0, offlineRoute.getInstructions().size() - 1);

                /* We take the points from the instructions, so we can associate them with the corresponding instruction
                Each instruction matches a group of points on the path
                If we simply took them from resp.getPoints(), we would have to connect instructions and points in a clunky way */
                for (Instruction currentInstruction : instructionList) {

                    // first we get a list of points corresponding to the current instruction
                    PointList instructionPointList = currentInstruction.getPoints();

                    // for each one of those points, we associate a RoutePoint instance
                    for (int i = 0; i < instructionPointList.size(); i++) {

                        RoutePoint newPoint = new RoutePoint(
                                new GeoPoint(
                                        instructionPointList.getLatitude(i),
                                        instructionPointList.getLongitude(i)));

                        newPoint.setTrafficLevel(-1); // offline routes have -1 traffic level

                        // we also set the DirIcon. NOTE: START, ADJUSTED and DESTINATION are set by the NavPath constructor
                        switch (currentInstruction.getSign()) {
                            case 0:
                                newPoint.setIcon(RoutePoint.DirIcon.CONTINUE);
                                break;
                            case -1:
                                newPoint.setIcon(RoutePoint.DirIcon.TURN_SLIGHT_LEFT);
                                break;
                            case 1:
                                newPoint.setIcon(RoutePoint.DirIcon.TURN_SLIGHT_RIGHT);
                                break;
                            case -2:
                                newPoint.setIcon(RoutePoint.DirIcon.TURN_LEFT);
                                break;
                            case 2:
                                newPoint.setIcon(RoutePoint.DirIcon.TURN_RIGHT);
                                break;
                            case -3:
                                newPoint.setIcon(RoutePoint.DirIcon.TURN_SHARP_LEFT);
                                break;
                            case 4:
                                newPoint.setIcon(RoutePoint.DirIcon.TURN_SHARP_RIGHT);
                                break;
                            case -7:
                                newPoint.setIcon(RoutePoint.DirIcon.KEEP_LEFT);
                                break;
                            case 7:
                                newPoint.setIcon(RoutePoint.DirIcon.KEEP_RIGHT);
                                break;
                            case 6:
                                newPoint.setIcon(RoutePoint.DirIcon.ROUNDABOUT);
                                break;
                            case -6:
                                newPoint.setIcon(RoutePoint.DirIcon.LEAVE_ROUNDABOUT);
                                break;
                            default:
                                newPoint.setIcon(RoutePoint.DirIcon.UNKNOWN);
                                break;
                        }

                        if (i == 0) { // if it is the first instruction of the set
                            String instruction = currentInstruction.getTurnDescription(language);
                            newPoint.setInstruction(instruction); // we add the instruction text to it
                        }

                        newPoint.setStreetName(currentInstruction.getName()); // we set the street name

                        routePoints.add(newPoint);
                    }
                }


                // we assign street names
                if (offlineRoute.getPathDetails().containsKey("street_name")){
                    List<PathDetail> streetNameDetails = offlineRoute.getPathDetails().get("street_name");
                    for (PathDetail streetNameDetail : streetNameDetails) {

                        // we iterate through the min and max of each path detail
                        for (int i = streetNameDetail.getFirst(); i < streetNameDetail.getLast() + 1; i ++ ) {
                            if (i < routePoints.size()) { // if the point exists in our list
                                routePoints.get(i).setStreetName(streetNameDetail.getValue().toString());
                            }
                        }
                    }
                }

                // we do the exact same for the speed limits
                if (offlineRoute.getPathDetails().containsKey("average_speed")){
                    List<PathDetail> averageSpeedDetails = offlineRoute.getPathDetails().get("average_speed");
                    for (PathDetail averageSpeedDetail : averageSpeedDetails) {

                        // we iterate through the min and max of each path detail
                        for (int i = averageSpeedDetail.getFirst(); i < averageSpeedDetail.getLast() + 1; i ++ ) {
                            if (i < routePoints.size()) { // if the point exists in our list
                                routePoints.get(i).setRecommendedSpeed(averageSpeedDetail.getValue().toString().replaceFirst("\\.0", "")); // removing ".0"
                            }
                        }
                    }
                }

            } catch (Exception e) { // if anything goes wrong during the calculation
                routePoints.clear(); // we erase the whole path so we return returnExtremeFailure() in returnRouteResult()
                triggerOfflineRouteEvent.returnFailure();

                Log.e(TAG, "ERROR: Exception while calculating offline route: " + e.getMessage());
                e.printStackTrace();
            }

            return routePoints;
        }

        protected void onPostExecute(List<RoutePoint> routePoints) {
            if(routePoints == null || routePoints.isEmpty()) {
                triggerOfflineRouteEvent.returnFailure();
            } else {
                triggerOfflineRouteEvent.returnSuccess(routePoints);
            }
        }
    }


    /* Route Post Processing
    -------------------------------------------------- */
    public interface PostProcessCallback {
        void onFinishPostProcessing(double calculatedPathDistance);
    }

    /**
     * Performs some asynchronous calculations over the given route to fill missing street names,
     * add recommended speeds for each point and calculate each point's 0distance from its previous
     * point on the route
     * @param pathPoints the points from a route that was just calculated
     * @param instructionPoints the instruction points from the just calculated route
     * @param postProcessCallback a callback to be called after post processing
     */
    public void fillMissingPathInfo(List<RoutePoint> pathPoints, List<RoutePoint> instructionPoints, PostProcessCallback postProcessCallback) {
        // Finding and filling missing path data is done async
        FillMissingPathDataTask fillTask = new FillMissingPathDataTask(pathPoints, instructionPoints, postProcessCallback);
        fillTask.execute();
    }



    private static class FillMissingPathDataTask extends AsyncTask<Void, Void, Double> {

        List<RoutePoint> points;
        List<RoutePoint> instructions;
        PostProcessCallback postProcessCallback;

        FillMissingPathDataTask(List<RoutePoint> pathPoints, List<RoutePoint> instructionPoints, PostProcessCallback callback) {
            this.points = pathPoints;
            this.instructions = instructionPoints;
            this.postProcessCallback = callback;
        }

        @Override
        protected Double doInBackground(Void... voids) {
            Log.e(TAG, "Post-processing route...");
            double pathDistance = assignDistancesToPoints(points);
            assignPointDetails(points, instructions, hopper);
            return pathDistance;
        }

        @Override
        protected void onPostExecute(Double distance) {
            Log.e(TAG, "Finished post-processing route. Total distance is " + distance);
            super.onPostExecute(distance);
            postProcessCallback.onFinishPostProcessing(distance);
        }
    }

    /**
     * Assigns recommended speeds to each path RoutePoint, as well as potential street names if they
     * are an instruction point and do not have one assigned yet.
     * NOTE: This process has low performance
     * @param points the path points to be processed
     * @param instructions the path's instruction points\
     * @param hopper a GraphHopper instance to perform offline calculations
     */
    private static void assignPointDetails(@NonNull List<RoutePoint> points, @NonNull List<RoutePoint> instructions, @NonNull GraphHopper hopper) {

        FlagEncoder vehicleEncoder = hopper.getEncodingManager().getEncoder("car");
        String lastSpeedLimit = "";

        for (RoutePoint rp : points) {

            // we only assign these details to the first and last points, as well as every instruction point
            if (points.indexOf(rp) == 0
                    || points.indexOf(rp) == points.size() - 1
                    || instructions.contains(rp)) {

                // we avoid these calculations if there is no need for them
                if (rp.getRecommendedSpeed().isEmpty() || rp.getStreetName().isEmpty()) {
                    // we find the speed limit by retrieving the closest road first
                    QueryResult road = hopper.getLocationIndex()
                            .findClosest(
                                    rp.getGeoPoint().getLatitude(),
                                    rp.getGeoPoint().getLongitude(),
                                    EdgeFilter.ALL_EDGES);

                    if (road != null && road.getClosestEdge() != null) {
                        if (rp.getRecommendedSpeed().isEmpty()) {
                            String speedLimit = String.valueOf(vehicleEncoder.getSpeed(road.getClosestEdge().getFlags())).split("\\.")[0];
                            rp.setRecommendedSpeed(speedLimit);
                            lastSpeedLimit = speedLimit;
                        }

                        // if the current Route Point has no street name, we try to assign one from the same query result
                        if (rp.getStreetName().isEmpty()) {
                            rp.setStreetName(road.getClosestEdge().getName());
                        }
                    }
                } else {
                    // if the current RoutePoint already had an associated speed limit, we keep it
                    lastSpeedLimit = rp.getRecommendedSpeed();
                }

            } else {
                /* all other points will just have the speed limit associated
                There is no need to calculate street names. This greatly improves performance*/
                rp.setRecommendedSpeed(lastSpeedLimit);
            }
        }
    }

    /**
     * Assigns to each point the distance from its previous point
     * @param points the list of path points to assign distances to
     * @return the total path distance
     */
    private static double assignDistancesToPoints(@NonNull List<RoutePoint> points) {

        double totalDistance = 0.0;

        for (RoutePoint rp : points) {

            int currentIndex = points.lastIndexOf(rp);

            // if there is a previous point and no distance was assigned yet
            if (currentIndex != 0) {
                double distance = points.get(currentIndex - 1).getGeoPoint() // previous point
                        .sphericalDistance(rp.getGeoPoint()); // current point

                rp.setDistanceFromPreviousPoint(distance);

            }
            totalDistance += rp.getDistanceFromPreviousPoint(); // debug info
        }
        Log.d(TAG, "Total path distance -> " + totalDistance + "m");

        return totalDistance;
    }
}

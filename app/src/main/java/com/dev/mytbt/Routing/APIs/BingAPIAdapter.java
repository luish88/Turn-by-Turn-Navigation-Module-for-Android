package com.dev.mytbt.Routing.APIs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dev.mytbt.NavConfig;
import com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute.BingItineraryItem;
import com.dev.mytbt.Routing.APIs.BingAPI.Models.GetRoute.BingRouteLeg;
import com.dev.mytbt.Routing.APIs.BingAPI.Models.SnapToRoads.BingSnappedPoint;
import com.dev.mytbt.Routing.APIs.BingAPI.Responses.BingRespGetRoute;
import com.dev.mytbt.Routing.APIs.BingAPI.Responses.BingRespSnapToRoad;
import com.dev.mytbt.Routing.RoutePoint;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Luís Henriques for MyTbt.
 * 12-01-2018, 12:07
 */

@SuppressWarnings("UnusedDeclaration")
public class BingAPIAdapter extends RoutingAPIAdapter {

    private static final String TAG = "tbtBingAPI";

    @Override
    public void calcRoute(final GeoPoint origin, final GeoPoint destination, double bearing, CallbackOnlineRouteReceiver callback){

        // first we convert the GeoPoint objects to strings
        final String origLatLong = origin.getLatitude() + "," + origin.getLongitude();
        String destLatLong = destination.getLatitude() + "," + destination.getLongitude();

        // converting the heading to a string
        String heading = String.valueOf(bearing).split("\\.")[0];

        // then we make the request to Bing API
        BingRequests mapsService = RoutingAPIs.getInstance().connectToBingMaps();
        Call<BingRespGetRoute> responseGetRoute =
                mapsService.requestDirections(
                        origLatLong,
                        destLatLong,
                        heading,
                        NavConfig.getLanguageTag(),
                        NavConfig.API_KEY);

        // enqueuing the response
        responseGetRoute.enqueue(new Callback<BingRespGetRoute>() {
            @Override
            public void onResponse(@NonNull Call<BingRespGetRoute> call, @NonNull Response<BingRespGetRoute> response) {

                if (response.isSuccessful() && response.body() != null) {
                    // once we get a successful response...
                    List<RoutePoint> routePoints = new ArrayList<>(); // we start a new list of RoutePoints, that will be returned

                    try {
                        // we get a list of all the route points
                        List<GeoPoint> routeGeoPoints = response.body()
                                .getBingRouteResourceSets()[0]
                                .getResources()[0]
                                .getRoutePath()
                                .getLine()
                                .getLinePoints();

                        // then we get a list of all pertinent data from the Bing API
                        BingRouteLeg[] bingRouteLegs = response.body()
                                .getBingRouteResourceSets()[0]
                                .getResources()[0]
                                .getRouteLegs();

                        List<BingItineraryItem> allItems = new ArrayList<>();
                        for (BingRouteLeg currentLeg : bingRouteLegs) {
                            // adding all itinerary items on the same list (itinerary items encapsulate traffic info, instructions, ...)
                            allItems.addAll(Arrays.asList(currentLeg.getItineraryItems()));
                        }

                        // we create a final version of our items list
                        final List<BingItineraryItem> allInstructions = new ArrayList<>(allItems);

                        // finally, we create a string with all route point coordinates to feed the snap to roads request
                        String pointsString = "";
                        for (GeoPoint point : routeGeoPoints) {
                            pointsString = pointsString.concat(String.valueOf(point.getLatitude()))
                                    .concat(",")
                                    .concat(String.valueOf(point.getLongitude()))
                                    .concat(";");
                        }
                        // for the last point, we simply remove the final ";"
                        pointsString = pointsString.substring(0, pointsString.length() - 1);

                        Log.d(TAG, "Bing request Get Route was successful. Requesting snap to roads.");
                        // once we get a successful response, we make a new snap to roads request
                        Call<BingRespSnapToRoad> responseSnapToRoad =
                                mapsService.requestSnapToRoad(
                                        pointsString,
                                        NavConfig.API_KEY);

                        // enqueuing the snap to roads response
                        responseSnapToRoad.enqueue(new Callback<BingRespSnapToRoad>() {

                            @Override
                            public void onResponse(Call<BingRespSnapToRoad> call, Response<BingRespSnapToRoad> response) {

                                try {

                                    BingSnappedPoint[] snappedPoints
                                            = response.body()
                                            .getBingSnapResourceSets()[0]
                                            .getResources()[0]
                                            .getSnappedPoints();

                                    List<RoutePoint> allRoutePoints = new ArrayList<>();

                                    // now we build each RoutePoint
                                    for (BingSnappedPoint point : snappedPoints) {

                                        double latitude = point.getCoordinates().getLatitude();
                                        double longitude = point.getCoordinates().getLongitude();

                                        GeoPoint gp = new GeoPoint(latitude, longitude);
                                        RoutePoint newRP = new RoutePoint(gp);

                                        newRP.setStreetName(point.getStreetName());
                                        newRP.setRecommendedSpeed(String.valueOf(point.getSpeedLimit()));

                                        allRoutePoints.add(newRP);
                                    }

                                    /* we end up with two parallel lists. One with all the points (allRoutePoints) and another
                                    with all the instruction data (allInstructions). We have to join them together. We'll cycle
                                    through all instructions and, for each one, we'll find the closest RoutePoint. Once we do,
                                    we simply assign all the instructions to that point */

                                    for (BingItineraryItem currentItem : allInstructions) {

                                        // for each item, we find its closest RoutePoint
                                        RoutePoint closestPoint = findClosestRoutePoint(currentItem, allRoutePoints);

                                        // and complement it
                                        if (closestPoint != null)
                                            complementRoutePointFromItem(closestPoint, currentItem);

                                    }
                                    callback.returnSuccess(allRoutePoints);

                                } catch (Exception e) {
                                    Log.e(TAG, "Exception while retrieving snap to roads: " + response.message());
                                    Log.e(TAG, "Processing original request");
                                    e.printStackTrace();

                                    // On failure, we process our original response, without the snapped points
                                    List<RoutePoint> originalResponseRPs = processOriginalResponse(routeGeoPoints, allInstructions);
                                    callback.returnSuccess(originalResponseRPs);
                                }
                            }

                            @Override
                            public void onFailure(Call<BingRespSnapToRoad> call, Throwable throwable) {
                                Log.e(TAG, "ERROR while retrieving Bing snap to roads request. Processing original request");
                                // On failure, we process our original response, without the snapped points
                                List<RoutePoint> originalResponseRPs = processOriginalResponse(routeGeoPoints, allInstructions);
                                callback.returnSuccess(originalResponseRPs);
                            }
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Exception while retrieving route: " + response.message());
                        e.printStackTrace();
                        callback.returnFailure(origin, destination); // calculate an offline route on failure
                    }
                } else {
                    Log.e(TAG, "Route request not accepted: " + response.message());
                    callback.returnFailure(origin, destination); // calculate an offline route on failure
                }
            }
            @Override
            public void onFailure(@NonNull Call<BingRespGetRoute> call, @NonNull Throwable t) {
                Log.w(TAG, "No connection.");
                callback.returnFailure(origin, destination); // calculate an offline route on failure
            }
        });
    }

    private List<RoutePoint> processOriginalResponse(List<GeoPoint> originalGPs, List<BingItineraryItem> allInstructions) {
        List<RoutePoint> allRoutePoints = new ArrayList<>();

        for (GeoPoint gp : originalGPs) {
            allRoutePoints.add(new RoutePoint(gp));
        }

        for (BingItineraryItem currentItem : allInstructions) {

            // for each item, we find its closest RoutePoint
            RoutePoint closestPoint = findClosestRoutePoint(currentItem, allRoutePoints);

            // and complement it
            if (closestPoint != null)
                complementRoutePointFromItem(closestPoint, currentItem);
        }
        return allRoutePoints;
    }

    /**
     * Finds the RoutePoint that is closest to the specified itinerary item
     * @param currentItem the item
     * @param routePoints a list of RoutePoints
     * @return returns the closest RoutePoint that has no instructions
     */
    @Nullable
    private static RoutePoint findClosestRoutePoint(BingItineraryItem currentItem, List<RoutePoint> routePoints) {

        double pointLat = currentItem.getManeuverPoint().getCoordinates()[0];
        double pointLng = currentItem.getManeuverPoint().getCoordinates()[1];

        GeoPoint point = new GeoPoint(pointLat, pointLng);

        RoutePoint closestPoint = null;
        double closestDist = Double.MAX_VALUE;

        for (RoutePoint rp : routePoints) {
            double currentDist = point.sphericalDistance(rp.getGeoPoint());

            if (currentDist < closestDist) {
                if (rp.getStreetName().isEmpty() && rp.getInstruction().isEmpty()) {
                    closestDist = currentDist;
                    closestPoint = rp;
                }
            }
        }
        return closestPoint;
    }

    /**
     * Complements a RoutePoint with the information on a BingItineraryItem
     * @return a RoutePoint with all the information
     */
    @SuppressWarnings("UnusedReturnValue")
    private static RoutePoint complementRoutePointFromItem(RoutePoint routePoint, BingItineraryItem currentItem) {

        try {
            String instruction = "";
            String traffic = "";
            String streetName = "";
            String warningType = "";

            // if it is, we get the data from our response
            if (currentItem.getInstruction() != null)
                instruction = currentItem.getInstruction().getText();

            if (currentItem.getWarnings() != null)
                traffic = currentItem.getWarnings()[0].getSeverity();

            if (currentItem.getDetails() != null
                    && currentItem.getDetails().length > 0
                    && currentItem.getDetails()[0].getNames() != null
                    && currentItem.getDetails()[0].getNames().length > 0)
                streetName = currentItem.getDetails()[0].getNames()[0];

            if (currentItem.getWarnings() != null
                    && currentItem.getWarnings()[0] != null
                    && currentItem.getWarnings()[0].getWarningType() != null) {
                    warningType = currentItem.getWarnings()[0].getWarningType();
            }

            // and add each element, if it exists. We also set the markers in order, so the most important ones override the less important ones
            if (!instruction.isEmpty()) { // instructions
                routePoint.setInstruction(instruction);
                routePoint.setIcon(
                        processInstruction(
                                currentItem.getInstruction().getManeuverType()));
            }

            if (!warningType.isEmpty()) {
                switch (warningType.toLowerCase()) {
                    case "accident":
                        routePoint.setMarker(RoutePoint.MarkerType.ACCIDENT);
                        break;
                    case "blockedroad":
                        routePoint.setMarker(RoutePoint.MarkerType.BLOCKED_ROAD);
                        break;
                    case "congestion":
                    case "trafficflow":
                        routePoint.setMarker(RoutePoint.MarkerType.TRAFFIC);
                        break;
                    case "masstransit":
                        routePoint.setMarker(RoutePoint.MarkerType.MASS_TRANSIT);
                        break;
                    case "disabledvehicle":
                        routePoint.setMarker(RoutePoint.MarkerType.DISABLED_VEHICLE);
                        break;
                    case "roadhazard":
                        routePoint.setMarker(RoutePoint.MarkerType.ROAD_HAZARD);
                        break;
                    case "scheduledconstruction":
                        routePoint.setMarker(RoutePoint.MarkerType.CONSTRUCTION);
                        break;
                    default:
                        // routePoint.setMarker(RoutePoint.MarkerType.OTHER); // NOTE: Consider! We are ignoring some warnings
                        Log.e(TAG, "Ignored warning type: " + warningType);
                        break;
                }
            }

            // traffic data
            if (!traffic.isEmpty()) {
                // Severity can have the following values: Low Impact, Minor, Moderate, or Serious.
                switch (traffic.toLowerCase()) {
                    case "low impact":
                        routePoint.setTrafficLevel(0);
                        break;
                    case "minor":
                        routePoint.setTrafficLevel(1);
                        routePoint.setMarker(RoutePoint.MarkerType.NONE); // we do this so we won't repeat markers
                        break;
                    case "moderate":
                        routePoint.setTrafficLevel(2);
                        routePoint.setMarker(RoutePoint.MarkerType.NONE); // we do this so we won't repeat markers
                        break;
                    case "serious":
                        routePoint.setTrafficLevel(3);
                        routePoint.setMarker(RoutePoint.MarkerType.NONE); // we do this so we won't repeat markers
                        break;
                }
                // any other case will leave the value at 0
            }

            if (!streetName.isEmpty()) {
                routePoint.setStreetName(streetName);
            }

        } catch (NullPointerException e) {
            Log.e(TAG, "onResponse()");
            e.printStackTrace();
        }
        return routePoint;
    }

    /**
     * Translates bing's instructions to our instructions
     * @param instructionType an instruction from the Bing API
     * @return the appropriate DirIcon
     */
    private static RoutePoint.DirIcon processInstruction(String instructionType) {
        String instructionIcon = instructionType.toLowerCase(); // converting to lower case to simplify .contains()

        if (instructionIcon.contains("keep") && instructionIcon.contains("left")) {
            return RoutePoint.DirIcon.KEEP_LEFT;
        } else if (instructionIcon.contains("keep") && instructionIcon.contains("right")) {
            return RoutePoint.DirIcon.KEEP_RIGHT;
        } else if (instructionIcon.contains("continue") || instructionIcon.contains("straight")) {
            return RoutePoint.DirIcon.UNKNOWN; // We want to ignore simple "continue" instructions
        } else if (instructionIcon.contains("enter") && instructionIcon.contains("roundabout")) {
            return RoutePoint.DirIcon.ROUNDABOUT;
        } else if (instructionIcon.contains("roundabout")) {
            return RoutePoint.DirIcon.LEAVE_ROUNDABOUT;
        } else if (instructionIcon.contains("left")) {
            return RoutePoint.DirIcon.TURN_LEFT;
        } else if (instructionIcon.contains("right")) {
            return RoutePoint.DirIcon.TURN_RIGHT;
        } else {
            return RoutePoint.DirIcon.UNKNOWN;
        }
    }
}

package com.dev.mytbt.Routing.APIs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dev.mytbt.Routing.APIs.OsrmAPI.OsrmRespGetRoute;
import com.dev.mytbt.Routing.APIs.OsrmAPI.ResponseModels.OsrmStep;
import com.dev.mytbt.Routing.RoutePoint;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Luís Henriques for MyTbt.
 * 16-04-2018, 11:45
 */
public class OsrmAdapter extends RoutingAPIAdapter {

    private static final String TAG = "tbtOsrmAPI";

    @Override
    public void calcRoute(GeoPoint origin, GeoPoint destination, double bearing, CallbackOnlineRouteReceiver callback) {
        /* first we retrieve a string containing a range of directions for the current user bearing
        We also add a semicolon to the bearing list for each of the waypoints */
        String bearings = buildBearingString(bearing)
                .concat(";");

        /* then we build a string containing all points, starting with the origin.
        Lat and Lng must be reversed to match osrm's criteria */
        String points = String.valueOf(origin.getLongitude())
                .concat(",")
                .concat(String.valueOf(origin.getLatitude()))
                .concat(";")
                .concat(String.valueOf(destination.getLongitude()))
                .concat(",")
                .concat(String.valueOf(destination.getLatitude()));

        // now we enqueue the request
        OsrmRequests routingService = RoutingAPIs.getInstance().connectToOsrmAPI();
        Call<OsrmRespGetRoute> responseGetRoute =
                routingService.requestDirections(points, bearings);

        responseGetRoute.enqueue(new Callback<OsrmRespGetRoute>() {
            @Override
            public void onResponse(@NonNull Call<OsrmRespGetRoute> call, @NonNull Response<OsrmRespGetRoute> response) {

                if (response.isSuccessful() && response.body() != null) {
                    List<RoutePoint> routePoints = createRoutePoints(response); // we create the RoutePoints from the response

                    if (routePoints == null || routePoints.isEmpty()) { // if we were not able to process the points
                        callback.returnFailure(origin, destination); // we calculate an offline route
                    } else {
                        // if not, we return success
                        callback.returnSuccess(routePoints);
                    }

                } else {
                    Log.e(TAG, "Route request not accepted: " + response.message());
                    callback.returnFailure(origin, destination); // calculate an offline route on failure
                }
            }

            @Override
            public void onFailure(@NonNull Call<OsrmRespGetRoute> call, @NonNull Throwable t) {
                Log.w(TAG, "No connection.");
                callback.returnFailure(origin, destination); // calculate an offline route on failure
            }
        });
    }

    /**
     * Creates a list of RoutePoints from an API response
     * @param response the response
     * @return a list of RoutePoints properly populated, or null if any error occurred
     */
    @Nullable
    private List<RoutePoint> createRoutePoints(Response<OsrmRespGetRoute> response) {
        // once we get a success response...
        List<RoutePoint> routePoints = new ArrayList<>(); // we create a new list of RoutePoints

        try {
            // first we populate the list of RoutePoints with lat and lng values from the list of coordinates we received
            List<double[]> allCoordinates = response.body().getRoutes()[0].getGeometry().getCoordinates();

            for (double[] coordinate : allCoordinates) {
                GeoPoint gp = new GeoPoint(coordinate[1], coordinate[0]); // NOTE: Osrm's coordinates are inverted
                routePoints.add(new RoutePoint(gp));
            }

            // then we populate each maneuver point with the info from the corresponding step
            try {
                OsrmStep[] allSteps = response.body().getRoutes()[0].getLegs()[0].getSteps();

                for (OsrmStep step : allSteps) {

                    if (step.getManeuver() != null) {

                        // we find the RoutePoint that matches the given coordinates
                        RoutePoint currentRp = findPoint(step.getManeuver().getLocation()[1],
                                step.getManeuver().getLocation()[0],
                                routePoints);

                        if (currentRp != null) {
                            currentRp.setStreetName(step.getStreetName());

                            if (step.getManeuver() != null
                                    && step.getManeuver().getType() != null
                                    && step.getManeuver().getModifier() != null) {

                                // we simply join type and maneuver together to be converted to the appropriate icon
                                String instruction = step.getManeuver().getType().concat(" ").concat(step.getManeuver().getModifier());
                                currentRp.setIcon(processInstruction(instruction));

                                // if the current point is a roundabout, we set the appropriate exit
                                if (currentRp.getIcon().equals(RoutePoint.DirIcon.ROUNDABOUT)) {
                                    if (step.getManeuver().getExit() != null) {
                                        currentRp.setRoundaboutExit(
                                                Integer.parseInt(step.getManeuver().getExit()));
                                    }
                                }
                            }

                        }
                    }
                }
            } catch (NullPointerException npe) {
                Log.e(TAG, "ERROR onResponse: Route has no steps");
                return null; // we return a null list so we process a route calculation failure
            }

            return routePoints; // if everything went well so far, we return the list of routepoints

        } catch (Exception e) {
            Log.e(TAG, "Exception while processing route: " + response.message());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return returns a string with a 180º range for the user bearing
     */
    @NonNull
    private String buildBearingString(double bearing){

        int b = (int) bearing; // we retrieve the bearing of the user, cast as an integer

        // Clamping bearing just to prevent bugs
        if (b < 0) {
            Log.e(TAG, "ERROR: Bearing was negative! Value: " + b );
            b = 360 - Math.abs(b);
        }
        if (b > 360) {
            b = 360 - b;
        }

        // and set a 45º tolerance for our direction
        return String.valueOf(b)
                .concat(",45");
    }

    /**
     * Finds the point in the list that matches the given lat lng values
     * @param lat latitude
     * @param lng longitude
     * @param pointList the list of points containing the point to be found
     * @return the point that matches the given coordinates
     */
    @Nullable
    private RoutePoint findPoint(double lat, double lng, List<RoutePoint> pointList) {

        for (RoutePoint rp : pointList) {
            if (rp.getGeoPoint().getLatitude() == lat
                    && rp.getGeoPoint().getLongitude() == lng )
                return  rp;
        }

        Log.e(TAG, "ERROR: findPoint was unable to find a point matching the given coordinates ");

        return null;
    }

    /**
     * Translates Osrm's instructions to our instructions
     * @param instructionType an instruction from the Osrm API
     * @return the appropriate DirIcon
     */
    private static RoutePoint.DirIcon processInstruction(String instructionType) {
        String instructionIcon = instructionType.toLowerCase(); // converting to lower case to simplify .contains()

        // START, READJUSTED and DESTINATION are set by the NavPath constructor

        if (instructionIcon.contains("use lane") && instructionIcon.contains("left")) {
            return RoutePoint.DirIcon.KEEP_LEFT;
        } else if (instructionIcon.contains("use lane") && instructionIcon.contains("right")) {
            return RoutePoint.DirIcon.KEEP_RIGHT;
        } else if ((instructionIcon.contains("roundabout") && !instructionIcon.contains("turn"))
                || instructionIcon.contains("rotary") ) {
            return RoutePoint.DirIcon.ROUNDABOUT;

        } else if (instructionIcon.contains("on ramp")) {
            if (instructionIcon.contains("right")) {
                return RoutePoint.DirIcon.ON_RAMP_RIGHT;
            } else if (instructionIcon.contains("left")) {
                return RoutePoint.DirIcon.ON_RAMP_LEFT;
            }

        } else if (instructionIcon.contains("off ramp")) {
            if (instructionIcon.contains("right")) {
                return RoutePoint.DirIcon.OFF_RAMP_RIGHT;
            } else if (instructionIcon.contains("left")) {
                return RoutePoint.DirIcon.OFF_RAMP_LEFT;
            }

        } else if (instructionIcon.contains("sharp right")) {
            return RoutePoint.DirIcon.TURN_SHARP_RIGHT;
        } else if (instructionIcon.contains("sharp left")) {
            return RoutePoint.DirIcon.TURN_SHARP_LEFT;
        } else if (instructionIcon.contains("slight right")) {
            return RoutePoint.DirIcon.TURN_SLIGHT_RIGHT;
        } else if (instructionIcon.contains("slight left")) {
            return RoutePoint.DirIcon.TURN_SLIGHT_LEFT;
        } else if (instructionIcon.contains("right")) {
            return RoutePoint.DirIcon.TURN_RIGHT;
        } else if (instructionIcon.contains("left")) {
            return RoutePoint.DirIcon.TURN_LEFT;
        } else if (instructionIcon.contains("uturn")) {
            return RoutePoint.DirIcon.U_TURN;
        } else if (instructionIcon.contains("straight")) {
            return RoutePoint.DirIcon.UNKNOWN; // We want to ignore simple "continue" instructions
        }
        return RoutePoint.DirIcon.UNKNOWN; // by default, we return UNKNOWN
    }
}

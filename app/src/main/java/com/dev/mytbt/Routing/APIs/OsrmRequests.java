package com.dev.mytbt.Routing.APIs;

import com.dev.mytbt.Routing.APIs.OsrmAPI.OsrmRespGetRoute;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by Luís Henriques for MyTbt.
 * 16-04-2018, 12:01
 */
@SuppressWarnings("UnusedDeclaration")
public interface OsrmRequests {

    /**
     * Requests a route from the Osrm routing API
     * @param points a string containing both the starting position and all other path waypoints, separated by semicolons.
     *              The coordinates must be REVERSED, stating longitude values first, like so: "longitude,latitude".
     *              E.g: "-8.41426833333,40.1913166667;-8.412748,40.193168;-8.421080,40.192848"
     * @param bearings a string containing the desired bearings for each point, with the exception of the last one (the destination).
     *                 Each bearing entry must state a direction in degrees and a range of possible directions. The number of
     *                 (direction + range) pairs must match the number of points minus the destination (2 pairs for the
     *                 3 points in our example). If the default values are to be used, simply leave an empty field.
     *                 E.g. in which we only state the bearing (350) and the range od degrees (180º range) for the
     *                 first of our 3 points: "bearings=350,180;;"
     * @return A response model containing the route data
     */
    @GET("car/{points}?steps=true&geometries=geojson&overview=full")
    Call<OsrmRespGetRoute> requestDirections(@Path("points") String points,
                                             @Query("bearings") String bearings);
}

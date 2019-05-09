package com.dev.mytbt.Routing.APIs;

import com.dev.mytbt.Routing.APIs.BingAPI.Responses.BingRespGetRoute;
import com.dev.mytbt.Routing.APIs.BingAPI.Responses.BingRespSnapToRoad;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by Luís Henriques for MyTbt.
 * 20-11-2017, 10:18
 */

@SuppressWarnings("UnusedDeclaration")
public interface BingRequests {

    // get optimized route
    @GET("Routes/Driving/?routeAttributes=routePath&optimize=timeWithTraffic")
    Call<BingRespGetRoute> requestDirections(@Query("wayPoint.1") String origLatLong,
                                             @Query("wayPoint.2") String destLatLong,
                                             @Query("heading") String heading,
                                             @Query("culture") String languageTag,
                                             @Query("key") String APIKey);

    // snap a group of points to roads
    @GET("Routes/SnapToRoad/?interpolate=true&includeSpeedLimit=true&speedUnit=KPH&travelMode=driving")
    Call<BingRespSnapToRoad> requestSnapToRoad(@Query("points") String allPoints,
                                               @Query("key") String APIKey);

}
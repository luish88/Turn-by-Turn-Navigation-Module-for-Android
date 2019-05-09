package com.dev.mytbt.Routing.APIs;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Luís Henriques for MyTbt.
 * 20-11-2017, 10:15
 */

class RoutingAPIs {

    // Singleton
    private static RoutingAPIs instance = null;
    private RoutingAPIs() {}
    static RoutingAPIs getInstance() {
        if(instance == null)
            instance = new RoutingAPIs();

        return instance;
    }

    private BingRequests bingService;
    BingRequests connectToBingMaps() {
        if (bingService == null) { // ensures only one instance of BingRequests exists
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://dev.virtualearth.net/REST/v1/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            bingService = retrofit.create(BingRequests.class);
        }
        return bingService;
    } // connects to Bing Maps API

    private OsrmRequests osrmService;
    OsrmRequests connectToOsrmAPI() {
        if (osrmService == null) { // ensures only one instance of OsrmRequests exists
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("SET URL HERE")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            osrmService = retrofit.create(OsrmRequests.class);
        }
        return osrmService;
    } // connects to an Osrm Routing API
}

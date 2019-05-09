package com.dev.mytbt.Tools;

import android.content.Context;

import com.dev.mytbt.R;

import org.oscim.layers.vector.geometries.Style;

/**
 * Created by Luís Henriques for MyTbt.
 * 26-01-2018, 10:48
 */

public class RouteStyles {

    /**
     * Returns a style depending on the given traffic level
     * @param trafficLevel between -1 (offline) and 3 (serious)
     * @param appContext The current application context getContext()
     */
    public static Style getStyleForTrafficLevel(int trafficLevel, Context appContext){
            switch (trafficLevel){ // line color depends on the traffic level
                case -1:
                    return RouteStyles.offlineTraffic(appContext);
                case 1:
                    return RouteStyles.minorTraffic(appContext);
                case 2:
                    return RouteStyles.moderateTraffic(appContext);
                case 3:
                    return RouteStyles.seriousTraffic(appContext);
                default: // default includes value of 0
                    return RouteStyles.noTraffic(appContext);
            }
    }


    private static Style offlineTraffic(Context appContext){
        return Style.builder()
                .fixed(true)
                .generalization(Style.GENERALIZATION_SMALL)
                .strokeColor(appContext.getResources().getColor(R.color.trafficOffline))
                .strokeWidth(4 * appContext.getResources().getDisplayMetrics().density)
                .build();
    }


    private static Style noTraffic(Context appContext) {
        return Style.builder()
                .fixed(true)
                .generalization(Style.GENERALIZATION_SMALL)
                .strokeColor(appContext.getResources().getColor(R.color.trafficNone))
                .strokeWidth(4 * appContext.getResources().getDisplayMetrics().density)
                .build();
    }


    private static Style minorTraffic(Context appContext) {
        return Style.builder()
                .fixed(true)
                .generalization(Style.GENERALIZATION_SMALL)
                .strokeColor(appContext.getResources().getColor(R.color.trafficMinor))
                .strokeWidth(4 * appContext.getResources().getDisplayMetrics().density)
                .build();
    }


    private static Style moderateTraffic(Context appContext) {
        return Style.builder()
                .fixed(true)
                .generalization(Style.GENERALIZATION_SMALL)
                .strokeColor(appContext.getResources().getColor(R.color.trafficModerate))
                .strokeWidth(4 * appContext.getResources().getDisplayMetrics().density)
                .build();
    }


    private static Style seriousTraffic(Context appContext) {
        return Style.builder()
                .fixed(true)
                .generalization(Style.GENERALIZATION_SMALL)
                .strokeColor(appContext.getResources().getColor(R.color.trafficSerious))
                .strokeWidth(4 * appContext.getResources().getDisplayMetrics().density)
                .build();
    }
}

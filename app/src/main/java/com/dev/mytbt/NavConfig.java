package com.dev.mytbt;

import android.os.Environment;

import com.dev.mytbt.Routing.APIs.BingAPIAdapter;
import com.dev.mytbt.Routing.APIs.RoutingAPIAdapter;

import org.oscim.theme.VtmThemes;

import java.io.File;
import java.util.Locale;

/**
 * Created by Luís Henriques for MyTbt.
 * 16-09-2018, 13:55
 */
public class NavConfig {

    // Language
    public static Locale getLanguage() {
        return Locale.getDefault();
    }

    // Routing API
    public static RoutingAPIAdapter getRoutingAPIAdapter() {
        return new BingAPIAdapter(); // NOTE: Set the API adapter here
    }
    public static final String API_KEY = "SET YOUR BING MAPS API KEY HERE";

    // GPS Cycle (in milliseconds)
    public static final int GPS_CYCLE = 1000;

    // Map files
    private static final File MAPS_ROOT_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    private static final String MAPS_BASE_DIR = "/TbtModuleMaps/";
    public static final File MAPS_FOLDER = new File(MAPS_ROOT_DIR, MAPS_BASE_DIR);

    // Look
    static final VtmThemes MAP_THEME = VtmThemes.DEFAULT;

    // Overview camera settings
    static final int MIN_OVERVIEW_ZOOM = 100; // how close can the user be in overview mode, so 3D buildings do not pop out. Default: 100
    static final int DEFAULT_OVERVIEW_MAX_ZOOM = 68243; // when changing to Overview mode, the camera can't be closer than this or it will be reset. 68243
    static final int DEFAULT_OVERVIEW_MIN_ZOOM = 19985; // when changing to Overview mode, the camera can't be further away than this or it will be reset. Default: 19985
    static final int ZOOM_OUT_ZOOM = 5000; // the zoom value for when zooming out before focusing a point on the map

    // Turn-by-Turn camera settings
    static final Double DEFAULT_TBT_ZOOM = 264916.0; // min: 131072.0, max: 1048576.0, default: 264916.0
    static final int MIN_TBT_ZOOM = 17; // min zoom set to min 3D level: 17 (so it never displays 2D)
    static final Double DEFAULT_TBT_MAX_ZOOM = 1048576.0; // Clamp values for TbT. when changing to TbT mode, the camera can't be closer than this or it will be reset. Default: 1048576.0
    static final Double DEFAULT_TBT_MIN_ZOOM = 131072.0; // // when changing to TbT mode, the camera can't be further away than this or it will be reset. Default: 131072.0
    static final float TBT_PORTRAIT_SCREEN_CENTER = 0.8f; // sets the center of the screen to be at the bottom, for navigation
    static final float TBT_LANDSCAPE_SCREEN_CENTER = 0.7f;
    static final int REFOCUS_SECONDS = 3000; // the number of milliseconds it takes for the camera to refocus the user after he touches the map

    // Turn-by-Turn Navigation config
    public static final double WARM_RADIUS = 50; // the max distance (in meters) the user can be to a path point and still be considered as following the path // Default = 20
    public static final double GREEN_ZONE_CLOSE_EDGE = 2000; // the distance, in meters, that is teh start of the green zone
    public static final double GREEN_ZONE_FAR_EDGE = 5000; // the distance, in meters, that is the end of the green zone

    /**
     * @return Returns the tag of the current selected language
     * E.g: Portugal will return "pt-PT"
     *      England will return "en-EN"
     */
    public static String getLanguageTag() {
        String languageTag = getLanguage().toString();
        languageTag = languageTag.replace("_", "-");
        return languageTag;
    }
}

package com.dev.mytbt.Tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

/**
 * Created by Luís Henriques for MyTbt.
 * 18-09-2018, 20:01
 */
public class NavSharedPrefs {

    private static final String SP_KEY_USER = "user";
    private static final String LAST_SELECTED_MAP = "lastSelectedMap";

    /**
     * Saves the name of the last selected map in shared preferences
     * @param mapName the last selected map name
     * @param context a context object to access shared preferences
     */
    public static void saveMapChoice(String mapName, Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                SP_KEY_USER,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(LAST_SELECTED_MAP, String.valueOf(mapName));
        editor.apply();
    }

    /**
     * Loads the last selected map from shared preferences
     * @param context a context object to access shared preferences
     * @return returns the name of the folder of the last selected map
     */
    @NonNull
    public static String loadLastSelectedMap(Context context) {

        SharedPreferences sharedPref = context.getSharedPreferences(
                SP_KEY_USER,
                Context.MODE_PRIVATE);

        return sharedPref.getString(LAST_SELECTED_MAP, "");
    }
}

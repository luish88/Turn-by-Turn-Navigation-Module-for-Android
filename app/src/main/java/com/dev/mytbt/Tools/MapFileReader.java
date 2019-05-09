package com.dev.mytbt.Tools;

import com.dev.mytbt.NavConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luís Henriques for MyTbt.
 * 16-09-2018, 13:54
 */
public class MapFileReader {
    /**
     * Checks if all map files for a given country are present in the device
     * @param country the specified country
     * @return whether all files exists or not
     */
    private static boolean checkIfMapFilesExist(String country) {

        File mapsFolder = NavConfig.MAPS_FOLDER;
        boolean allFilesWereFound = true; // default return value

        if (mapsFolder.exists()) {

            country = country.toLowerCase();

            File areaFolder = new File(mapsFolder, country);

            if (areaFolder.exists()) {
                List<File> allFiles = new ArrayList<>();

                // all these files must be present in order for a map to be validated
                allFiles.add(new File(areaFolder, "edges"));
                allFiles.add(new File(areaFolder, "geometry"));
                allFiles.add(new File(areaFolder, "location_index"));
                allFiles.add(new File(areaFolder, "names"));
                allFiles.add(new File(areaFolder, "nodes"));
                allFiles.add(new File(areaFolder, "nodes_ch_fastest_car"));
                allFiles.add(new File(areaFolder, country + ".map"));
                allFiles.add(new File(areaFolder, "properties"));
                allFiles.add(new File(areaFolder, "shortcuts_fastest_car"));

                for (File f : allFiles) {
                    if (!f.exists()) {
                        allFilesWereFound = false;
                        break;
                    }
                }
                return allFilesWereFound;
            }
        }
        return false;
    }

    /**
     * @return returns a list of all the names of the maps found in the maps directory which are complete (aka: All required files are present)
     */
    public static List<String> getMapsInDirectory() {
        // check file integrity
        File mapsFolder = NavConfig.MAPS_FOLDER;
        List<String> validMaps = new ArrayList<>(); // a list containing all valid map folders

        // first we check if the base map folder exists
        if (mapsFolder.exists()) {
            // if it does, we retrieve a list of all map folders
            String[] maps = mapsFolder.list((current, name) -> new File(current, name).isDirectory());

            // and check if each of the folders has all necessary files
            if (maps != null && maps.length > 0) {
                for (String map : maps) {

                    if (checkIfMapFilesExist(map) && map.length() > 1) {

                        // converting the first letter to capital letter
                        String firstLetter = map.substring(0, 1);
                        firstLetter = firstLetter.toUpperCase();
                        map = map.substring(1);
                        map = firstLetter.concat(map);

                        validMaps.add(map); // we add each valid folder to this list
                    }
                }
            }
        }
        return validMaps;
    }
}

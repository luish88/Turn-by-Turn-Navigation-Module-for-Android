package com.dev.mytbt;

import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dev.mytbt.Guide.NavPathLayer;
import com.dev.mytbt.Guide.NavPathMarker;
import com.dev.mytbt.Routing.RoutePoint;
import com.dev.mytbt.Tools.NavSharedPrefs;
import com.dev.mytbt.Tools.RouteStyles;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;

import org.oscim.android.MapView;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.layers.vector.PathLayer;
import org.oscim.tiling.source.mapfile.MapFileTileSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luís Henriques for MyTbt.
 * 19-09-2018, 19:55
 *
 */
class MapViewController extends ViewModel {

    private static final String TAG = "tbtMapViewModel"; // Debug tag

    // Map modes
    static final int MAP_MODE_OVERVIEW = 0;
    static final int MAP_MODE_TURN_BY_TURN = 1;
    private static int currentMapMode = MAP_MODE_OVERVIEW; // map mode starts as overview

    private MapFileTileSource tileSource;
    private ItemizedLayer<MarkerItem> userMarkerLayer;

    private GeoPoint userMarkerPosition = null;
    private MarkerItem userMarker = null;
    private boolean userMarkerHidden = false;

    private static List<NavPathLayer> currentPathLayers;
    private static List<NavPathMarker> currentPathMarkers;

    private boolean mapIsLoaded = false;

    private static MapPosition pausedMapPosition;

    /* Setup Actions
    ----------------------------------------------- */
    /**
     * Loads the map on the MapView
     * @param selectedMap the map that was selected to be loaded
     */
    void loadMap(@NonNull String selectedMap, @NonNull MapView mapView, @NonNull InitializeMapCallback initializeMapCallback, @NonNull MapEventsReceiver mapEventsReceiver, @NonNull Context c) {

        mapIsLoaded = false; // map is not loaded since we are resetting it

        // we also save the selected map on the shared prefs
        NavSharedPrefs.saveMapChoice(selectedMap, c);

        // so we start preparing the map
        Log.w(TAG, "Loading map for " + selectedMap + "...");

        /* we clear the whole map when we begin the setup process.
        We do so in order to prevent any unexpected events in case the user downloaded or deleted maps */
        if (mapView.map() != null && mapView.map().layers().size() > 1) {
            removeAllMapLayers(mapView);
            erasePathFromMap(mapView);
            mapView.map().updateMap(true);
        }

        /* Loading the map from the selected map files */
        tileSource = new MapFileTileSource();
        final File areaFolder = new File(NavConfig.MAPS_FOLDER, selectedMap);
        tileSource.setMapFile(new File(areaFolder, selectedMap + ".map").getAbsolutePath());

        restoreMap(mapView, mapEventsReceiver);

        // initializing maps as an AsyncTask
        InitializeMapCallback localCallback = new InitializeMapCallback() {
            @Override
            public void onSuccess(GraphHopper hopper) {
                mapIsLoaded = true; // and declare that the map is loaded
                Log.w(TAG, "... Map loaded successfully");

                initializeMapCallback.onSuccess(hopper); // and return the GraphHopper instance through the callback
            }

            @Override
            public void onFailure() {
                mapIsLoaded = false; // and declare that the map is not loaded
                Log.e(TAG, "ERROR: Loading graphs failed");

                initializeMapCallback.onFailure();
            }
        };
        InitializeMapsTask initializeMapsTask = new InitializeMapsTask(localCallback, selectedMap);
        initializeMapsTask.execute();
    }
    private static class InitializeMapsTask extends AsyncTask<Void, Void, Boolean> {

        InitializeMapCallback callback;
        String mapToLoad;
        GraphHopper newHopper = null;

        InitializeMapsTask(@NonNull InitializeMapCallback callback, @NonNull String mapToLoad) {
            this.callback = callback;
            this.mapToLoad = mapToLoad;
        }

        @SuppressWarnings("all") // supressing "condition is always true"
        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                // initializing map
                GraphHopper tmpHopp = new GraphHopper().forMobile();

                tmpHopp.getCHFactoryDecorator().setDisablingAllowed(true); // we allow disabling CH so we can process heading

                // setting up speed encoder
                FlagEncoder vehicleEncoder = null;
                vehicleEncoder = new CarFlagEncoder(); // Consider this! It is loading the graph for cars only

                tmpHopp.setEncodingManager(new EncodingManager(vehicleEncoder));

                tmpHopp.load(new File(NavConfig.MAPS_FOLDER, mapToLoad).getAbsolutePath());

                newHopper = tmpHopp;

            } catch (Exception e) {
                e.getMessage();
                e.printStackTrace();
                return false;
            }
            return true;
        }

        protected void onPostExecute(Boolean success) {
            if (success) {
                callback.onSuccess(newHopper);
            } else {
                callback.onFailure();
            }
        }
    }

    public interface InitializeMapCallback {

        void onSuccess(GraphHopper hopper);

        void onFailure();
    }

    /**
     * Restores all map elements, layers, markers and position
     * @param mapEventsReceiver a layer to process user input
     */
    void restoreMap(MapView mapView, @NonNull MapEventsReceiver mapEventsReceiver) {

        if (mapView.map().layers().size() > 1) {
            removeAllMapLayers(mapView);
        }

        /* Instantiating map layers */
        VectorTileLayer l = mapView.map().setBaseMap(tileSource); // setting the base tile source on the map
        BuildingLayer buildingLayer = new BuildingLayer(mapView.map(), l);
        LabelLayer labelLayer = new LabelLayer(mapView.map(), l);
        userMarkerLayer = new ItemizedLayer<>(mapView.map(), (MarkerSymbol) null); // user marker layer

        /* (Re)Setting map elements */
        // if we have a map position that was passed while pausing the activity / fragment, we reset it
        if (pausedMapPosition != null) {
            mapView.map().setMapPosition(pausedMapPosition);
            pausedMapPosition = null; // releasing the saved map position
        }

        // if we have a user position (AKA: the map view is reloading)
        if (userMarkerPosition != null) {
            setUserMarkerPosition(userMarkerPosition, mapView); // we refresh it on the map
        }

        // Adding the new layers (in order of appearence)
        mapView.map().layers().add(buildingLayer);
        mapView.map().layers().add(labelLayer);
        mapView.map().layers().add(mapEventsReceiver);

        // Setting map theme (Can only be set after the tile source)
        mapView.map().setTheme(NavConfig.MAP_THEME);

        // if we have a path, we draw it on the map
        if (currentPathLayers != null) {
            drawPathLayers(currentPathLayers, mapView);
        } else {
            Log.e(TAG, "restoreMap: LAYERS NULL" );
        }
        if (currentPathMarkers != null)
            drawMarkerLayers(currentPathMarkers, mapView);


        /* layers must be added in the proper order, so the user marker layer must be added
        in the end, so the user marker stays on top of everything else */
        mapView.map().layers().add(userMarkerLayer);

        mapView.map().updateMap(true);
    }

    /**
     * @return returns whether or not the map is fully loaded
     */
    boolean isMapIsLoaded() {
        return mapIsLoaded;
    }

    /**
     * Saves the current map position for reloading
     * @param mapPosition the map position to be saved
     */
    void pauseMapPosition(MapPosition mapPosition){
        pausedMapPosition = mapPosition;
    }


    /* Map Interaction
    ----------------------------------------------- */
    /**
     * Processes user map interaction
     */
    public static abstract class MapEventsReceiver extends Layer implements GestureListener {

        MapEventsReceiver(org.oscim.map.Map map) {
            super(map);
        }

        @Override
        public abstract boolean onGesture(Gesture g, MotionEvent e);

    } // layer for when the user touches the map


    /* Map Elements drawing
    ----------------------------------------------- */
    /**
     * Updates the user marker positon on the map
     */
    void setUserMarkerPosition(@NonNull GeoPoint position, @NonNull MapView mapView){

        // first, we store the user's position, so we can reset it when we refresh the map
        userMarkerPosition = position;

        // if we are in overview mode
        if (!userMarkerHidden) {
            // we remove the marker from the layer
            if (userMarker != null) {
                userMarkerLayer.removeItem(userMarker);
            }

            // update the marker and its position
            userMarker = getUserMarker(mapView.getContext(), position);

            // and add it again
            userMarkerLayer.addItem(userMarker);
        }
    }

    /**
     * Hides the user marker on the map.
     * NOTE: This does not update the MapView
     * To redraw the user marker, use showUserMarker()
     */
    void hideUserMarker() {
        if (userMarker != null && userMarkerLayer != null) {
            userMarkerLayer.removeItem(userMarker);
            userMarkerHidden = true;
        }
    }

    /**
     * Shows the user marker on the map.
     * NOTE: This does not update the MapView
     */
    void showUserMarker(MapView mapView) {
        if (userMarker != null && userMarkerLayer != null) {
            userMarkerLayer.removeItem(userMarker); // removing any previous marker from the map
            userMarker = getUserMarker(mapView.getContext(), userMarkerPosition);
            userMarkerLayer.addItem(userMarker);
            userMarkerHidden = false;
        }
    }

    /**
     * Draws a given set of path layers on the map
     */
    private void drawPathLayers(List<NavPathLayer> pathLayers, MapView mapView) {

        // first we convert from our model to true PathLayer objects
        List<PathLayer> newLayers = new ArrayList<>();
        for (NavPathLayer npl : pathLayers) {
            PathLayer pl = new PathLayer(mapView.map(),
                            RouteStyles.getStyleForTrafficLevel(npl.getTrafficLevel(),
                            mapView.getContext()));
            pl.addPoints(npl.getPoints());
            newLayers.add(pl);
        }

        // then we add them to our map
        mapView.map().layers().addAll(newLayers);
    }

    /**
     * Draws a given set of markers on the map
     */
    private void drawMarkerLayers(List<NavPathMarker> pathMarkers, MapView mapView) {

        List<MarkerItem> newMarkers = new ArrayList<>();
        for (NavPathMarker mk : pathMarkers) {
            MarkerItem newMarker = createMarkerItem(
                    mapView.getContext(),
                    mk.getPoint(),
                    RoutePoint.getMarkerIconResource(mk.getMarkerType()));

            newMarkers.add(newMarker);
        }

        ItemizedLayer<MarkerItem> infoMarkerLayer = new ItemizedLayer<>(mapView.map(), (MarkerSymbol) null);
        infoMarkerLayer.addItems(newMarkers);
        mapView.map().layers().add(infoMarkerLayer);
    }

    /**
     * Erases the map by completely clearing all layers from it
     */
    private void removeAllMapLayers(MapView mapView) {
        Log.w(TAG, "Clearing old map layers...");

        /* NOTE: Removing layers from index 0 causes a lot of problems, namely
        the map tiles will not be rendered and map interaction becomes impossible.
        Never found out why. */
        List<Layer> obsoleteLayers = new ArrayList<>();
        for ( int i = 1; i < mapView.map().layers().size(); i ++ ) {
            mapView.map().layers().get(i).setEnabled(false);
            obsoleteLayers.add(mapView.map().layers().get(i));
        }
        mapView.map().layers().removeAll(obsoleteLayers);

        Log.w(TAG, "...map layers removed. " +  mapView.map().layers().size() + " layer(s) remained.");
    }

    /**
     * Completely erases the current path from the map view, as well as its elements
     */
    void erasePathFromMap(MapView mapView) {

        /* Clearing all path elements from the map means
        clearing all the path lines, as well as all the
        information markers */

        // so first we're gonna try to find them
        List<Layer> pathLayers = new ArrayList<>();
        List<Layer> infoMarkerLayers = new ArrayList<>();

        for (Layer l : mapView.map().layers()) {
            if (l.getClass().equals(PathLayer.class)) { // we check for the class of each layer on the map
                pathLayers.add(l); // if it is a path layer, we collect it
            }

            // if we find a marker layer that is not the user marker layer
            else if (l.getClass().equals(ItemizedLayer.class) && !l.equals(userMarkerLayer)) {
                // it's because it's an info marker layer
                infoMarkerLayers.add(l);
            }
        }

        // after collecting all the layers we need, we simply remove them
        mapView.map().layers().removeAll(infoMarkerLayers);
        mapView.map().layers().removeAll(pathLayers);

        currentPathLayers = null;
        currentPathMarkers = null;
    }


    /* Building Map Elements
    ----------------------------------------------- */
    @Nullable
    private MarkerItem createMarkerItem(Context context, GeoPoint p, int resource) {
        MarkerItem markerItem = null;
        try {
            Drawable drawable = context.getResources().getDrawable(resource);
            Bitmap bitmap = AndroidGraphics.drawableToBitmap(drawable);

            MarkerSymbol markerSymbol = new MarkerSymbol(bitmap, 0.5f, 0.9f, true);
            markerItem = new MarkerItem("", "", p);
            markerItem.setMarker(markerSymbol);
        } catch (Exception e) {
            // Log.w(TAG, "ERROR: createMarkerItem: " + e.getMessage() + " -> Are we running tests?" );
        }
        return markerItem;
    } // creates a new marker from an existing resource

    private MarkerItem getUserMarker(Context context, GeoPoint point) {
        return createMarkerItem(context, point, R.drawable.mk_user);
    }


    /* Path
    ----------------------------------------------- */
    /**
     * Erases all path elements from the map and draws a fresh path
     * @param pathLayers A list of path layers to add to the map
     * @param markerLayers A list of path markers to add to the map
     * @param mapView the MapView on which the path should be drawn
     */
    void drawPathOnMap(@NonNull List<NavPathLayer> pathLayers, @NonNull List<NavPathMarker> markerLayers, MapView mapView) {

        erasePathFromMap(mapView);

        currentPathLayers = pathLayers;
        currentPathMarkers = markerLayers;

        drawPathLayers(pathLayers, mapView);
        drawMarkerLayers(markerLayers, mapView);

        /* Redrawing the user marker layer so it stays on top */
        mapView.map().layers().remove(userMarkerLayer);
        mapView.map().layers().add(userMarkerLayer);

        mapView.map().updateMap(true);
    }

    /* Gets & Sets
        ----------------------------------------------- */
    int getCurrentMapMode() {
        return currentMapMode;
    }

    void setCurrentMapMode(int currentMapMode) {
        MapViewController.currentMapMode = currentMapMode;
    }
}

package com.dev.mytbt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.dev.mytbt.Guide.NavPathLayer;
import com.dev.mytbt.Guide.NavPathMarker;
import com.dev.mytbt.Guide.NavigationController;
import com.dev.mytbt.Guide.NavigationGuide;
import com.dev.mytbt.Guide.RouteDetail;
import com.dev.mytbt.Guide.RouteDetailsAdapter;
import com.dev.mytbt.Routing.RoutePoint;
import com.dev.mytbt.Tools.GpsService;
import com.dev.mytbt.Tools.MapFileReader;
import com.dev.mytbt.Tools.NavSharedPrefs;
import com.dev.mytbt.Tools.PermissionManager;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.graphhopper.GraphHopper;

import org.oscim.android.MapView;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Gesture;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.vector.PathLayer;
import org.oscim.utils.Easing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class FragNavigation extends Fragment {

    private static final String TAG = "tbtFragNavigation"; // Debug tag

    private BroadcastReceiver gpsReceiver;
    private static MapViewController mapViewController; // this controller is shared by all instances of this fragment
    private static boolean gpsDetectedForTheFirstTime = false;

    private static NavigationController navigationController;

    private View navBar;
    private View vRouteDetails;
    private LinearLayout llUserArrow;
    private static MapView mapView;
    private ImageButton btnFocusRoute;
    private static AlertDialog disabledGpsDialog;
    private static ProgressDialog waitingGpsDialog;
    private ProgressBar pbLoading;
    private AlertDialog driveDialog;
    private AlertDialog mapsDialog;

    private ImageView ivTbtNbDirIcon;
    private TextView tvTbtNbDistance;
    private TextView tvTbtNbStreetName;
    private TextView tvTbtNbRecommendedSpeed;
    private TextView tvTbtNbTimeDist;

    private static boolean mayInteract = true; // may the user interact with the UI?

    // request code for searching places
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 0;

    // Camera
    private float tbtScreenCenter = NavConfig.TBT_PORTRAIT_SCREEN_CENTER;
    private boolean userPressedMap = false;
    private int focusCameraCounter = 0;

    // Turn by turn arrow - change this comment
    private boolean showArrowOnNextCycle = false;


    /* Instantiation
    ----------------------------------------------- */
    public FragNavigation() {} // Required empty public constructor

    /**
     * @return A new instance of fragment FragNavigation.
     */
    public static FragNavigation newInstance() {
        return new FragNavigation();
    }


    /* Lifecycle states
    ----------------------------------------------- */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_navigation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupLayout(view);
    }

    @Override
    public void onResume() {
        super.onResume();

        setOrientationOptions(); // NOTE: MUST run before running checks
        runChecks();

        if (mapView != null)
            mapView.onResume();
    }

    @Override
    public void onPause() {

        if (navigationController != null)
            navigationController.pauseNavigation();

        // preventing dialog memory leaks
        if (driveDialog != null && driveDialog.isShowing())
            driveDialog.dismiss();

        if (mapsDialog != null && mapsDialog.isShowing())
            mapsDialog.dismiss();

        // Unregistering the GPS receiver from this fragment
        Context c = getContext();
        if (gpsReceiver != null && c != null){
            try { // the receiver may not be registered
                c.unregisterReceiver(gpsReceiver);
            } catch (IllegalArgumentException iaex) {
                Log.e(TAG, "ERROR: Exception while trying to unregister gpsReceiver onPause(): " + iaex.getMessage() );
            }
        }

        if (mapView != null) {
            // saving the map position in memory so it is restored on the next fragment instance
            mapViewController.pauseMapPosition(mapView.map().getMapPosition());
            mapView.onPause();
        }
        /* Releasing UI elements
        Since views and UI elements are bound to each fragment instance, and every time we pause or
        rotate the device, a new fragment instance is created, we simply dismiss all dialogs when
        a fragment is about to leave.
        Al necessary dialogs will simply be created and shown as needed onResume() */
        popGpsDisabledPrompt(false);
        popWaitingGpsPrompt(false);

        super.onPause();
    }

    @Override
    public void onDestroy() {

        Activity a = getActivity();
        if (GpsService.isRunning(a)) {
            Intent gpsService = new Intent(a.getApplicationContext(), GpsService.class);
            a.stopService(gpsService);
        }

        if (mapView != null)
            mapView.onDestroy();

        super.onDestroy();
    }

    /* Setup
    ----------------------------------------------- */
    private void setupLayout(View v) {
        // Setting up the MapView and navigation layout
        mapView = v.findViewById(R.id.mapView);
        navBar = v.findViewById(R.id.navBar);
        llUserArrow = v.findViewById(R.id.llUserArrow);
        pbLoading = v.findViewById(R.id.pbThrobber);
        vRouteDetails = v.findViewById(R.id.vRouteDetails);

        ivTbtNbDirIcon = navBar.findViewById(R.id.ivTbtNbDirIcon);
        tvTbtNbDistance = navBar.findViewById(R.id.tvTbtNbDistance);
        tvTbtNbStreetName = navBar.findViewById(R.id.tvTbtNbStreetName);
        tvTbtNbRecommendedSpeed = navBar.findViewById(R.id.tvTbtNbRecommendedSpeed);
        tvTbtNbTimeDist = navBar.findViewById(R.id.tvTbtNbTimeDist);

        /* Setting up prompts
        Whenever we refresh the fragments, the dialogs start hidden */
        popGpsDisabledPrompt(false);
        popWaitingGpsPrompt(false);

        /* NOTE:
        We have to build the dialogs when refreshing the fragment. Sadly, we have to keep them in memory.
        This is because if the user deactivates the location service (GPS) through the Quick Settings dropdown
        menu, the app is on the background, and we do not have a Context object to help us build the dialogs.
        In other words, getContext() always returns null when the user selects an option on the Quick Settings
        menu. So we are not able to build the dialogs on the spot. We can call them though, and show them or
        hide them as needed. So we build them preemptively */
        Context c = getContext();
        if (c != null) {

            // "Waiting for GPS" dialog
            waitingGpsDialog = new ProgressDialog(c);
            waitingGpsDialog.setMessage("\n".concat(c.getString(R.string.waitingGpsTitle)));
            waitingGpsDialog.setIndeterminate(true);
            waitingGpsDialog.setCancelable(false);

            // "Gps service off" dialog
            AlertDialog.OnClickListener buttonListener = (dialog, which) -> {
                // "GPS disabled" click listener, sends the user to the correct menu so he can activate the location service
                popGpsDisabledPrompt(false);
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            };
            android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(c);
            dialogBuilder.setTitle(c.getString(R.string.promptNoGpsTitle));
            dialogBuilder.setMessage(c.getString(R.string.promptNoGpsText));
            dialogBuilder.setPositiveButton(c.getString(R.string.genericOk), buttonListener);
            dialogBuilder.setCancelable(false);
            disabledGpsDialog = dialogBuilder.create();
        }

        // Setting up the navBar
        navBar.setOnClickListener(v12 -> onNavBarPress());

        // Setting up the navigation buttons
        btnFocusRoute = v.findViewById(R.id.btnViewFullRoute);
        btnFocusRoute.setOnClickListener(v1 -> onBtnFocusRoutePress());
        ImageButton btnFocusUser = v.findViewById(R.id.btnFocusUser);
        btnFocusUser.setOnClickListener(v2 -> onBtnFocusPress());
        ImageButton btnChangeMode = v.findViewById(R.id.btnChangeMode);
        btnChangeMode.setOnClickListener(v3 -> onBtnChangeModePress());

                    // this call is required to make sure we place the listener on the correct view
        v.setFocusableInTouchMode(true);
        v.requestFocus();

        v.setOnKeyListener((v4, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {

                // we filter all actions that are not key down
                if (event.getAction() != KeyEvent.ACTION_DOWN)
                    return true;

                if (vRouteDetails.getVisibility() == View.VISIBLE) {
                    popRouteDetails(false);
                    return false;
                } else {
                    if (getActivity() != null)
                        getActivity().onBackPressed(); // by default, we simply use the activity's onBackPressed()

                    return false;
                }
            }
            return false;
        });

    }

    /**
     * Runs the setup or resumes navigation if everything is ready
     */
    public void runChecks() {

        /* Checking map data model
        --------------------------------------- */
        if (mapViewController == null) // If there is no mapViewController, we instantiate one that is shared among all instances of this fragment
            mapViewController = ViewModelProviders.of(this).get(MapViewController.class);

        /* Checking required permissions
        --------------------------------------- */
        if (!PermissionManager.hasAllPermissions()) {
            Log.w(TAG, "runChecks() stopped. User didn't grant all permissions. " );
            return;
        }

        /* Checking map files, initializing Hopper and navigation system
        --------------------------------------- */
        Context c = getContext();
        if (c != null) {

            /* Checking map files */
            List<String> mapsInDirectory = MapFileReader.getMapsInDirectory();
            if (mapsInDirectory.isEmpty()) { // if there are no maps in the directory

                AlertDialog.OnClickListener buttonListener = (dialog, which) -> {
                    if (getActivity() != null)
                        getActivity().finish(); // closing app on pressing ok

                };
                // No maps dialog
                android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(c);
                dialogBuilder.setTitle(c.getString(R.string.promptNoMapFilesTitle));
                dialogBuilder.setMessage(c.getString(R.string.promptNoMapFilesText).concat("\n").concat(NavConfig.MAPS_FOLDER.getAbsolutePath()));
                dialogBuilder.setPositiveButton(c.getString(R.string.genericOk), buttonListener);
                dialogBuilder.setCancelable(false);
                mapsDialog = dialogBuilder.create();
                mapsDialog.show();

                return;
            } else {
                // if there are valid maps in the directory
                String loadedMap = NavSharedPrefs.loadLastSelectedMap(c); // we get the last selected map

                /* Checking map state */
                if (!loadedMap.equals("") // if we have a selected map
                        && mapsInDirectory.contains(loadedMap) // and its files are present and intact
                        && mapViewController.isMapIsLoaded()){  // and it is already loaded

                    /* Restore map upon resuming the app
                    NOTE: We use the same navigation events callback as well as the same GraphHopper instance */
                    mapViewController.restoreMap(mapView, createMapInteractionListeners());

                } else {
                    /* NOTE: Ideally, this only runs on application start on if the current map files are changed
                    while using the app */

                    // We present the loading map dialog
                    ProgressDialog dialogLoadingMap = new ProgressDialog(c);
                    dialogLoadingMap.setMessage("\n".concat(c.getString(R.string.promptLoadingMap)));
                    dialogLoadingMap.setIndeterminate(true);
                    dialogLoadingMap.setCancelable(false);

                    /* If we have to load the map from scratch, probably because the user is starting the app,
                    we create a callback for when the map is loaded */
                    MapViewController.InitializeMapCallback initializeMapCallback = new MapViewController.InitializeMapCallback() {
                        // This callback, upon loading the map, is responsible for setting up the navigation events
                        @Override
                        public void onSuccess(GraphHopper hopper) {
                            Log.w(TAG, "Map initialized successfully ");
                            NavigationGuide navGuide = new NavigationGuide(c);
                            navigationController = new NavigationController(hopper, navGuide, generateNavEventsTrigger());

                            // preventing trying to close the dialog after rotating the device
                            if (getActivity() != null && !getActivity().isDestroyed() && dialogLoadingMap.isShowing()) {
                                dialogLoadingMap.dismiss();
                            }
                        }

                        @Override
                        public void onFailure() {
                            Log.e(TAG, "ERROR initializing map.");

                            // but if we are unable to initialize the maps successfully
                            if (getActivity() != null && !getActivity().isDestroyed() && dialogLoadingMap.isShowing()) {
                                dialogLoadingMap.dismiss();
                            }

                            AlertDialog.OnClickListener buttonListener = (dialog, which) -> {
                                if (getActivity() != null)
                                    getActivity().finish(); // closing app on pressing ok

                            };
                            // Invalid maps dialog
                            android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(c);
                            dialogBuilder.setTitle(c.getString(R.string.promptInvalidMapFilesTitle));
                            dialogBuilder.setMessage(c.getString(R.string.promptInvalidMapFilesText).concat("\n").concat(NavConfig.MAPS_FOLDER.getAbsolutePath()));
                            dialogBuilder.setPositiveButton(c.getString(R.string.genericOk), buttonListener);
                            dialogBuilder.setCancelable(false);
                            mapsDialog = dialogBuilder.create();
                            mapsDialog.show();
                        }
                    };

                    // after preparing the callback for after initializing the map
                    if (mapsInDirectory.size() == 1) { // if we only have one map, which is not loaded
                        String map = mapsInDirectory.get(0); // we automatically select it
                        mapViewController.loadMap(map, mapView, initializeMapCallback, createMapInteractionListeners(), c); // and load it
                        dialogLoadingMap.show();

                    } else { // but if we have more than one map
                        AlertDialog.OnClickListener buttonListener = (dialog, which) -> {
                            String selectedMap = mapsInDirectory.get(which);
                            Log.w(TAG, "Selected map -> " + selectedMap );
                            mapViewController.loadMap(selectedMap, mapView, initializeMapCallback, createMapInteractionListeners(), c); // and load it
                            continueSetupAfterMapSelected(c);
                            mapsDialog.dismiss();
                        };

                        // populating the map selection spinner
                        ArrayAdapter<String> mapNamesAdapter = new ArrayAdapter<>(c, android.R.layout.simple_list_item_1, mapsInDirectory);

                        // Multiple maps dialog
                        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(c);
                        dialogBuilder.setTitle(c.getString(R.string.promptMultipleMapFilesTitle));
                        dialogBuilder.setSingleChoiceItems(mapNamesAdapter, 0, buttonListener);
                        dialogBuilder.setCancelable(false);
                        mapsDialog = dialogBuilder.create();
                        mapsDialog.show();
                        return;
                    }
                }
            }
            continueSetupAfterMapSelected(c);
        }
    }

    private void continueSetupAfterMapSelected(Context c) {
                    /* Checking GPS service
            --------------------------------------- */
        Activity a = getActivity();
        if (a != null && !GpsService.isRunning(a)) {
                /* Then we set up the GPS service. It must be set up here because the user may simply take away the Location permission
                while the app is paused, in which case, the gps service is simply destroyed. So we check if the service is running, and
                if it is not, we set it up */
            Intent gpsService = new Intent(c.getApplicationContext(), GpsService.class);
            c.startService(gpsService);
            Log.e(TAG, "Starting GPS service" );
        } else {
            Log.e(TAG, "GPS Service is already running" );
        }

            /* Registering GPS Receiver
            --------------------------------------- */
        // we register a new gps receiver that transmits data to this new instance of FragNavigation
        gpsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.d(TAG, "Receiving -->" + intent.getAction() );

                String action = intent.getAction();
                if ( action != null ) {
                    if (action.equals("gps_location_update")) {
                        onReceivingGpsData(intent);

                    } else if (action.equals("gps_status_changed")) {
                        onGpsStatusChange(intent);
                    }
                }
            }
        };
        // finally, we register the new gps receiver
        c.registerReceiver(gpsReceiver, new IntentFilter("gps_location_update"));
        c.registerReceiver(gpsReceiver, new IntentFilter("gps_status_changed"));

            /* Checking GPS
            --------------------------------------- */
        final LocationManager manager = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        if (manager != null) {
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { // if the GPS service is off
                Log.w(TAG, "GPS is disabled. Showing the \"GPS disabled\" prompt.");
                gpsDetectedForTheFirstTime = false; // we reset the GPS detected for the first time flag just to be through
                popGpsDisabledPrompt(true); // Showing the "no gps" prompt
                return;
            }
        } else {
            Log.e(TAG, "ERROR: checkHasGpsEnabled() failed. LocationManager not found.");
            return;
        }

            /* Check if GPS was already detected once
            --------------------------------------- */
        if (!gpsDetectedForTheFirstTime) {
            Log.w(TAG, "Waiting for GPS signal...");
            popWaitingGpsPrompt(true); // if we didn't detect GPS for the first time yet, we pop the "looking for GPS" prompt
        }

            /* Check if the text to speech is ready
            --------------------------------------- */
        if (navigationController != null && getContext() != null) {
            navigationController.resumeNavigation(getContext());
        }

            /* Update layout elements' state
            --------------------------------------- */
        updateFocusRouteButton();
        updateNavigationBarState();
        updateUserArrowState();

            /* Updating navController trigger instance and "loading" state
            --------------------------------------- */
        if (navigationController != null) { // if our navigation controller is set up
            navigationController.updateEventsTrigger(generateNavEventsTrigger());
                /* NOTE: We have to update the navigation controller's trigger instance (callback)
                every time we resume the app because the previous callback instance is bound
                to the previous FragNavigation instance's functions which, in turn, are bound
                to the previous app context. This causes issues with the view elements, namely
                pbLoading */

            if (navigationController.isCalculatingPath()) {
                // if we are calculating a path when the user resumes the app, we show the loading widget
                pbLoading.setVisibility(View.VISIBLE);
            }
        }

            /* Restoring map mode
            --------------------------------------- */
        if (mapViewController != null && mapView != null && navigationController != null) {
            // Restoring previous view mode
            if (mapViewController.getCurrentMapMode() == MapViewController.MAP_MODE_TURN_BY_TURN) {

                    /* making it so the user doesn't jst "disappear" from the screen when we rotate the device or
                    pause and resume the activity */
                if (llUserArrow.getVisibility() != View.VISIBLE)
                    mapViewController.showUserMarker(mapView);

                changeToMapMode(MapViewController.MAP_MODE_TURN_BY_TURN); // restoring turn by turn view
            } else {
                changeToMapMode(MapViewController.MAP_MODE_OVERVIEW); // restoring overview mode
            }
        }

    }

    private void popGpsDisabledPrompt(boolean show) {

        // preventing bugs
        if (disabledGpsDialog == null) {
            return;
        }

        if (show) {
            if (!disabledGpsDialog.isShowing()) {
                disabledGpsDialog.show();
            }
        } else {
            disabledGpsDialog.dismiss();
        }
    } // "No GPS" prompt

    private void popWaitingGpsPrompt(boolean show) {

        // preventing bugs
        if (waitingGpsDialog == null) {
            return;
        }

        if (show) {
            if (!waitingGpsDialog.isShowing()) {
                waitingGpsDialog.show();
            }

        } else {
             waitingGpsDialog.dismiss();
        }
    } // Detecting GPS prompt

    /**
     * Sets up touch event listeners for the map
     * @return Returns an event layer to be passed to the mapView's map instance
     */
    @SuppressLint("ClickableViewAccessibility")
    private MapViewController.MapEventsReceiver createMapInteractionListeners() {
        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) { // releasing the touch
                onMapTouchRelease();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) { // touching
                onMapTouch();
            }
            return false; // NOTE: This must return false, or else map interaction will become impossible
        });

        // we set up a map events layer, to register long presses on the map
        return new MapViewController.MapEventsReceiver(mapView.map()) {
            @Override
            public boolean onGesture(Gesture g, MotionEvent e) {
                if (g instanceof Gesture.LongPress) {
                    GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                    onMapLongPress(p);
                }
                return false;
            }
        };
    }

    /**
     * Sets up a new callback to deal with the navigation events from the current
     * FragNavigation instance
     * @return a callback instance relative to this current fragment, to be passed to
     * the navigation controller instance
     */
    private NavigationController.CallbackNavigationEvent generateNavEventsTrigger() {
        return new NavigationController.CallbackNavigationEvent() {
            @Override
            public void onNewRouteReceived(@NonNull List<NavPathLayer> pathLayers, @NonNull List<NavPathMarker> markerLayers) {
                onReceivingNewRoute(pathLayers, markerLayers);
            }

            @Override
            public void onNewRouteFailure() {
                onFailingNewRoute();
            }

            @Override
            public void onRouteUpdateReceived(@NonNull List<NavPathLayer> pathLayers, @NonNull List<NavPathMarker> markerLayers) {
                onReceivingRouteUpdate(pathLayers, markerLayers);
            }

            @Override
            public void onRouteUpdateFailure() {
                onFailingRouteUpdate();
            }

            @Override
            public void onDetourFromRoute() {
                onRouteDetour();
            }

            @Override
            public void onRouteCompleted() {
                onReachingDestination();
            }

        };
    }

    /**
     * Sets some configuration options for when the user rotates the device
     */
    @SuppressWarnings("All")
    private void setOrientationOptions() {

        /* As the android documentation states that
            <activity ... android:configChanges="orientation|keyboardHidden"
        is bad practice, we use onResume() to detect the device's orientation
        and set some configurations accordingly */
        try {
            // Hiding the top menu in landscape mode
            Activity activity = getActivity();
            if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ((AppCompatActivity)activity).getSupportActionBar().hide();
                tbtScreenCenter = NavConfig.TBT_LANDSCAPE_SCREEN_CENTER;
            } else {
                ((AppCompatActivity)activity).getSupportActionBar().show();
                tbtScreenCenter = NavConfig.TBT_PORTRAIT_SCREEN_CENTER;
            }

        } catch (NullPointerException npe) {
            Log.e(TAG, "ERROR on setOrientationOptions(): " + npe.getMessage() );
            npe.printStackTrace();
        }

    } // sets behaviour for when the device is rotated


    /* Navigation data and behaviour
    ----------------------------------------------- */
    public void onReceivingGpsData(Intent intent) {
        if (intent.getExtras() != null
                && mapView != null // preventing behaviour for when the app is receiving GPS signal but it is not being displayed
                && mapViewController.isMapIsLoaded()) { // we only perform an action if the map is loaded

            try {

                double receivedLat = Double.valueOf(Objects.requireNonNull(intent.getExtras().get("lat")).toString());
                double receivedLng = Double.valueOf(Objects.requireNonNull(intent.getExtras().get("long")).toString());

                GeoPoint receivedUserPos = new GeoPoint(receivedLat, receivedLng); // first we fetch the detected user position
                float userBearing = Float.valueOf(Objects.requireNonNull(intent.getExtras().get("bearing")).toString());
                float gpsAccuracy = Float.valueOf(Objects.requireNonNull(intent.getExtras().get("accuracy")).toString());

                // if the GPS signal wasn't detected yet, we declare that we are detecting the GPS for the first time
                if (!gpsDetectedForTheFirstTime) {
                    Log.w(TAG, "... GPS signal detected for the first time.");
                    gpsDetectedForTheFirstTime = true;
                    popWaitingGpsPrompt(false);

                    // We also focus the received user position with the proper zoom
                    focusCameraOverview(receivedUserPos, 1000, false);
                }

                performNavigationStep(receivedUserPos, userBearing, gpsAccuracy);

            } catch (NullPointerException e) {
                Log.w(TAG, "onReceivingGPSData: Failed reading GPS coordinates" + e.getMessage());
                e.printStackTrace();
            } catch (Exception ignore) {}

            // after receiving GPS data, we always refresh the map
            mapView.map().updateMap(true);
        }
    }

    /**
     * Defines what happens when the GPS service is changed by the user
     * @param intent the intent with the gps status info
     */
    public void onGpsStatusChange(Intent intent) {
        if (intent.getExtras() != null) {
            boolean enabled = intent.getExtras().getBoolean("enabled");
            if (enabled) { // if the user enabled the GPS
                gpsDetectedForTheFirstTime = false; // we reset the GPS detected for the first time flag
                popGpsDisabledPrompt(false); // we dismiss the "no gps" prompt

                /* and show the "waiting for GPS" prompt (we have to set it here,
                since enabling GPS through the Quick Settings menu doesn't run
                onResume() when refocusing the app)*/
                popWaitingGpsPrompt(true);

            } else {
                popGpsDisabledPrompt(true); // else, we display the "no gps" prompt
                gpsDetectedForTheFirstTime = false; // we reset the GPS detected for the first time flag
            }
        }
    }

    /**
     * Performs a navigation step, adjusting the layout, user marker position and recalculating the current path if appropriate
     */
    private void performNavigationStep(GeoPoint detectedUserPos, float userBearing, float gpsAccuracy) {
        if (navigationController != null && getContext() != null) { // preventing crashes

            // we find the user's position relative to the path
            NavigationController.PositionUpdate posUpdate = navigationController.pinpointUser(detectedUserPos, userBearing, gpsAccuracy, getContext());
            GeoPoint updatedUerPos = posUpdate.getUserPos();
            float updatedUserBearing = posUpdate.getUserBearing();

            // we set the user marker position on the map
            mapViewController.setUserMarkerPosition(updatedUerPos, mapView);

            // we also attempt to animate the camera if the user is in Turn by Turn mode
            attemptTurnByTurnStep(updatedUerPos, updatedUserBearing);

            // finally, we update the navigation bar fields in case there's a next instruction
            NavigationController.NextInstructionInfo navBarInfo = posUpdate.getNextInstructionInfo();
            if (navBarInfo != null) {
                updateNavigationBarFields(navBarInfo.getDistToInstruction(), navBarInfo.getRecommendedSpeed(), navBarInfo.getIcon(), navBarInfo.getStreetName());
                return;
            }
        }
        updateNavigationBarState();
    }

    /**
     * Sets a new destination on the navigator
     */
    private void setNewDestination(GeoPoint destination){
        if (getContext() != null) {
            mapViewController.erasePathFromMap(mapView);
            navigationController.calculateNewRouteTo(destination, getContext());
            pbLoading.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Displays the new path on the map
     */
    private void onReceivingNewRoute(@NonNull List<NavPathLayer> pathLayers, @NonNull List<NavPathMarker> markerLayers) {
        hideLoadingWidget();
        mapViewController.drawPathOnMap(pathLayers, markerLayers, mapView);
        updateFocusRouteButton();
        updateNavigationBarState();
        focusRoute();
    }

    /**
     * Updates the map to display the updated route
     */
    private void onReceivingRouteUpdate(@NonNull List<NavPathLayer> pathLayers, @NonNull List<NavPathMarker> markerLayers) {
        hideLoadingWidget();
        mapViewController.drawPathOnMap(pathLayers, markerLayers, mapView);
        updateFocusRouteButton();
        updateNavigationBarState();
    }

    private void onFailingNewRoute() {
        hideLoadingWidget();
        mapViewController.erasePathFromMap(mapView);
        Log.e(TAG, "ERROR onFailingNewRoute: Route calculation failed.");
    }

    private void onFailingRouteUpdate() {
        hideLoadingWidget();
        mapViewController.erasePathFromMap(mapView);
        Log.e(TAG, "ERROR onFailingRouteUpdate: Route calculation failed.");
    }

    private void onRouteDetour() {
        pbLoading.setVisibility(View.VISIBLE);
        updateNavigationBarState();
    }

    private void onReachingDestination() {
        mapViewController.erasePathFromMap(mapView);
        updateNavigationBarState();
    }

    /* UI Interactions
    ----------------------------------------------- */
    private void onMapTouch(){
        if (mayInteract) {
            userPressedMap = true;
            showArrowOnNextCycle = false;

            // if the is in TbT mode, we hide the user arrow and show the marker
            if (mapViewController.getCurrentMapMode() == MapViewController.MAP_MODE_TURN_BY_TURN) {
                llUserArrow.setVisibility(View.GONE);
                mapViewController.showUserMarker(mapView);
                mapView.map().updateMap(true);
            }
        }
    }

    private void onMapTouchRelease() {
        if (mayInteract) {
            userPressedMap = false;
            focusCameraCounter += 1;

            // if the user is in TbT mode, we start a countdown, by the end of which, we refocus the user
            final Handler handler = new Handler();
            handler.postDelayed(() -> {

                /* if the user didn't press the map in the meantime
                (don't worry, he'll release the touch eventually, so our camera is not stuck) */
                if (focusCameraCounter > 0) { // we perform this check so we can reset the counter safely
                    focusCameraCounter -= 1;
                }
                // if we are in Tbt mode, there are no more refocus handlers waiting, and the user didn't press the map in the meantime
                if (!userPressedMap && focusCameraCounter <= 0 && mapViewController.getCurrentMapMode() == MapViewController.MAP_MODE_TURN_BY_TURN) {
                    focusUser();
                }

            }, NavConfig.REFOCUS_SECONDS);
        }
    }

    private void onMapLongPress(GeoPoint pressPoint) {
        if (mayInteract) {
            // Showing the destination prompt in overview mode
            Context c = getContext();
            if (mapViewController.getCurrentMapMode() == MapViewController.MAP_MODE_OVERVIEW && c != null){
                LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (inflater != null) {

                    // building the dialog
                    android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(c);
                    dialogBuilder.setCancelable(true);
                    driveDialog = dialogBuilder.create();

                    // setting up the dialog view
                    @SuppressLint("InflateParams")
                    View v = inflater.inflate(R.layout.dialog_drive, null);
                    Button positiveButton = v.findViewById(R.id.btnDriveOk);
                    Button negativeButton = v.findViewById(R.id.btnDriveCancel);
                    RadioGroup radioGroup = v.findViewById(R.id.rgDrive);
                    RadioButton rbCancel = v.findViewById(R.id.rbDriveCancel);
                    rbCancel.setEnabled(navigationController.hasPath()); // this button is only active if we have a path

                    positiveButton.setOnClickListener(v1 -> {

                        // finding the index of the selected option
                        int selectedRb = radioGroup.getCheckedRadioButtonId();
                        View radioButton = radioGroup.findViewById(selectedRb);
                        int selectedRbIndex = radioGroup.indexOfChild(radioButton);

                        driveDialog.dismiss(); // we dismiss the dialog before running the other functions

                        switch (selectedRbIndex) {
                            case 0:
                                onDriveHerePress(pressPoint);
                                break;
                            case 1:
                                onSearchDestinationPress();
                                break;
                            case 2:
                                onCancelRoutePress();
                                break;
                        } // by default, we simply ignore the option and close the prompt
                    });
                    negativeButton.setOnClickListener(v2 -> driveDialog.dismiss());
                    driveDialog.setView(v);
                    driveDialog.show();
                }
            }
        }
    }

    private void onDriveHerePress(GeoPoint destination) {
        if (mayInteract) {
            setNewDestination(destination);
        }
    }

    private void onSearchDestinationPress() {
        if (mayInteract) {
            displaySearchDestination();
        }
    }

    private void onCancelRoutePress() {
        if (mayInteract) {

            navigationController.cancelCurrentRoute();

            mapViewController.erasePathFromMap(mapView);
            updateFocusRouteButton();
            updateNavigationBarState();
        }
    }

    private void onBtnFocusRoutePress(){
        if (mayInteract) {
            focusRoute();
        }
    }

    private void onBtnFocusPress(){
        if (mayInteract) {
            focusCameraCounter = 0; // if the user presses the focus button, we reset this counter
            focusUser();
        }
    }

    private void onBtnChangeModePress(){
        if (mayInteract) {
            if (mapViewController.getCurrentMapMode() == MapViewController.MAP_MODE_OVERVIEW) {
                changeToMapMode(MapViewController.MAP_MODE_TURN_BY_TURN);
            } else {
                changeToMapMode(MapViewController.MAP_MODE_OVERVIEW);
            }
        }
    }

    private void onNavBarPress(){
        if (mayInteract) {
            popRouteDetails(true);
        }
    }


    /* UI Behaviour
    ----------------------------------------------- */
    /**
     * Enables or disables map interaction
     */
    private void enableMapInteraction(boolean enable) {
        mapView.setActivated(enable);
        mapView.setClickable(enable);
        mapView.setFocusable(enable);
        mapView.setFocusableInTouchMode(enable);
        mayInteract = enable;
    }

    /**
     * Changes the current map mode
     */
    private void changeToMapMode(int newMapMode) {
        if (newMapMode == MapViewController.MAP_MODE_TURN_BY_TURN) { // Changing to MapMode Turn-by-Turn

            // first thing we do is change the mode to Tbt
            mapViewController.setCurrentMapMode(MapViewController.MAP_MODE_TURN_BY_TURN);

            enableMapInteraction(false);

            // updating layout elements
            updateNavigationBarState();
            updateFocusRouteButton(); // we hide the view full route button

            // setting up turn by turn viewport
            mapView.map().viewport().setMinZoomLevel(NavConfig.MIN_TBT_ZOOM); // zoom limits
            mapView.map().viewport().setMapScreenCenter(tbtScreenCenter);

            focusUser();

        } else { //Changing to MapMode Overview

            mapViewController.setCurrentMapMode(MapViewController.MAP_MODE_OVERVIEW); // first thing we do is change the mode to Overview

            // we deactivate controls for a second, while we restore the mode
            enableMapInteraction(false);
            final Handler handler = new Handler();
            handler.postDelayed(() -> enableMapInteraction(true), 1100);

            // we update the layout elements
            updateNavigationBarState();
            updateFocusRouteButton(); // displaying the focus route button, if appropriate
            updateUserArrowState();
            mapViewController.showUserMarker(mapView); // showing the user marker

            // setting up turn by turn viewport
            mapView.map().viewport().setMinZoomLevel(NavConfig.MIN_OVERVIEW_ZOOM); // we reset min zoom value
            mapView.map().viewport().setTilt(0f); // 3D off
            mapView.map().viewport().setMapScreenCenter(0f); // we reset the center of the screen

            focusUser(); // we focus the user
        }
        mapView.map().updateMap(false);
    }

    /**
     * Shows the focus route button if the user has a path and is in Overview mode
     * Else, hides the focus full route button
     * NOTE: The desired map mode should be set before running this function
     */
    private void updateFocusRouteButton() {
        Context c = getContext();

        if (btnFocusRoute != null) {
            if (navigationController != null){
                // if the user has a path and is in Overview mode
                if (navigationController.hasPath()
                        && mapViewController.getCurrentMapMode() == MapViewController.MAP_MODE_OVERVIEW) {

                    // we show the button
                    if (btnFocusRoute.getVisibility() != View.VISIBLE) { // preventing redundant animations
                        btnFocusRoute.setVisibility(View.VISIBLE);
                        if (c != null) { // we only play animations if we have a context
                            Animation anim = AnimationUtils.loadAnimation(c, R.anim.anim_grow_right_in);
                            btnFocusRoute.startAnimation(anim);
                        }
                    }

                } else { // else, we hide the button
                    if (btnFocusRoute.getVisibility() != View.GONE) { // preventing redundant animations
                        if (c != null) { // we only play animations if we have a context
                            Animation anim = AnimationUtils.loadAnimation(c, R.anim.anim_shrink_right_out);
                            btnFocusRoute.startAnimation(anim);
                            anim.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    btnFocusRoute.setVisibility(View.GONE);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {
                                }
                            });
                        } else {
                            btnFocusRoute.setVisibility(View.GONE);
                        }
                    }
                }
            } else {
                btnFocusRoute.setVisibility(View.GONE);
            }
        } else {
            Log.e(TAG, "ERROR on updateFocusRouteButton(): btnFocusRoute is null. ");
        }
    }

    /**
     * Shows or hides the Navigation Bar as required.
     * NOTE: The desired map mode should be set before running this function
     */
    private void updateNavigationBarState() {

        Animation anim = null;

        // if the user is in TbT mode and has a path
        if (mapViewController.getCurrentMapMode() == MapViewController.MAP_MODE_TURN_BY_TURN
                && navigationController.hasPath()) {

            if (navBar.getVisibility() != View.VISIBLE) { // we show the navigation bar in case it is hiding
                anim = AnimationUtils.loadAnimation(getContext(), R.anim.anim_slide_right_in);
                navBar.setVisibility(View.VISIBLE);
            }
        } else {
            // In any other event, and if the navigation bar is not hidden already, we hide the navigation bar
            if (navBar.getVisibility() != View.GONE) {
                anim = AnimationUtils.loadAnimation(getContext(), R.anim.anim_slide_right_out);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        navBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            }
        }

        if (anim != null)
            navBar.startAnimation(anim);
    }

    /**
     * Updates the navigation bar UI with the passed parameters
     */
    private void updateNavigationBarFields(double distToInstruction, String recommendedSpeed, RoutePoint.DirIcon dirIcon, String streetName) {
        if (recommendedSpeed.isEmpty() || recommendedSpeed.equals("0")) { // if there is no value to present, we hide the icon
            tvTbtNbRecommendedSpeed.setVisibility(View.GONE);
        } else {
            if (tvTbtNbRecommendedSpeed.getVisibility() != View.VISIBLE) { // if the icon is hidden, we show it again
                tvTbtNbRecommendedSpeed.setVisibility(View.VISIBLE);
            }
            tvTbtNbRecommendedSpeed.setText(recommendedSpeed);
        }

        // Instruction icon
        ivTbtNbDirIcon.setImageResource(RoutePoint.getInstructionIconResource(dirIcon));

        // Street name (may be empty)
        tvTbtNbStreetName.setText(streetName);

        // Distance to next instruction
        /*if the user is still at point 0, we do not calculate the distance, since instruction
        0 is "start" or "path readjusted", and it behaves in a weird way.*/
        if (distToInstruction > 0 && dirIcon != RoutePoint.DirIcon.START && dirIcon != RoutePoint.DirIcon.READJUSTED) {
            String distToNextInstruction = distanceToString(distToInstruction);
            if (!distToNextInstruction.isEmpty()) {
                distToNextInstruction = getResources().getString(R.string.navbar_distance_prefix) + " " + distToNextInstruction; // adding the prefix
            }
            tvTbtNbDistance.setText(distToNextInstruction);
        } else {
            // if the returned distance is -1, it means we are still post-processing the route, so we can't present distances yet
            tvTbtNbDistance.setText("");
        }

        // Overall distance to destination
        double distToWaypoint = navigationController.getDistanceToWaypoint();
        String stringDistToWaypoint = distanceToString(distToWaypoint);
        tvTbtNbTimeDist.setText(stringDistToWaypoint);

    }

    /**
     * Checks the conditions to show or hide the user arrow
     */
    private void updateUserArrowState() {
        if (mapViewController.getCurrentMapMode() == MapViewController.MAP_MODE_TURN_BY_TURN
                && focusCameraCounter <= 0) {
            // the user arrow is only updated on each gps cycle to prevent animation fidgeting
            showArrowOnNextCycle = true;
        } else {
            showArrowOnNextCycle = false;
            llUserArrow.setVisibility(View.GONE);
        }
    }

    /**
     * Hides the loading widget on the UI thread which prevents crashes due
     * to async task thread coordination
     */
    private void hideLoadingWidget() {
        Activity a = getActivity();
        if (a != null) {
            a.runOnUiThread(() -> pbLoading.setVisibility(View.GONE));
        }
    }

    private void displaySearchDestination() {
        try {
            // We use the PlaceAutocomplete to search for a location
            AutocompleteFilter filter = new AutocompleteFilter.Builder()
                    .setCountry(Locale.getDefault().getCountry()) // we also add a country filter
                    .build();

            Activity myActivity = getActivity();

            if (myActivity != null) {
                Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                        .setFilter(filter)
                        .build(myActivity);
                startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE); // the result is received by onActivityResult()
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /*
         * Receiving a "places" search result
         */
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK) {
            Activity myActivity = getActivity();
            if (myActivity != null) {
                Place place = PlaceAutocomplete.getPlace(myActivity, data);
                GeoPoint point = new GeoPoint(place.getLatLng().latitude, place.getLatLng().longitude);
                setNewDestination(point);
            } else {
                Log.e(TAG, "ERROR: onActivityResult() with requestCode PLACE_AUTOCOMPLETE_REQUEST_CODE was unable to find the Activity instance");
            }
        }
    }

    /**
     * pops the route details page
     *
     * @param show show or hide
     */
    public void popRouteDetails(boolean show) {
        Animation anim = null;

        try {
            // showing route details
            if (show) {
                Context c = getContext();
                if (c != null) {
                    // preventing redundant animations and weird effects (we only play the animation if needed)
                    if (vRouteDetails.getVisibility() != View.VISIBLE) {
                        anim = AnimationUtils.loadAnimation(getContext(), R.anim.anim_bottom_in);
                        vRouteDetails.setVisibility(View.VISIBLE);
                    }
                    mayInteract = false;

                    ListView lvRouteDetailsList = vRouteDetails.findViewById(R.id.lvRDetailsList);

                    // populating the layout
                    LinearLayout llDestination = vRouteDetails.findViewById(R.id.llRDetailsDestination);
                    TextView tvDestination = vRouteDetails.findViewById(R.id.tvRDetailsDestination);
                    String destination;


                    // retrieving route details for our current path
                    List<RouteDetail> routeDetails = navigationController.getRouteDetails(c);

                    if (!routeDetails.isEmpty() && !navigationController.isCalculatingPath()) {

                        // we create the adapter with the entries
                        RouteDetailsAdapter detailsAdapter = new RouteDetailsAdapter(c, routeDetails);
                        lvRouteDetailsList.setAdapter(detailsAdapter);

                        // focusing the first item of our path
                        int focusItem = routeDetails.size() - 1; // by default, we focus the last item (the start point)
                        for (RouteDetail detail : routeDetails) {

                            String distText = ""; // we always convert the distance to a string to be presented to the user
                            if (detail.getIcon() == RoutePoint.DirIcon.START) {
                                try {
                                    // we always return the "start" string, even though we calculate the distance between the user and this point
                                    distText = c.getString(R.string.route_details_start);
                                } catch (Exception ignore) {}
                            } else {
                                distText = distanceToString(detail.getDistance());
                            }
                            detail.setDistanceText(distText);

                            if (detail.isPassedBy()) { // once we find a point that was passed by
                                break;
                            }
                            // if not, we'll keep the focus on the last "not passed by" entry
                            focusItem = routeDetails.indexOf(detail);
                        }
                        lvRouteDetailsList.setSelectionFromTop(focusItem, lvRouteDetailsList.getHeight() / 2);

                        // Showing the destination
                        destination = navigationController.getDestinationName(); // retrieving destination from the instruction point at the top of the list
                        if (!destination.isEmpty()) { // if we do have a destination on our last point, we show it

                            llDestination.setVisibility(View.VISIBLE);
                            tvDestination.setText(
                                    getString(R.string.route_details_destination)
                                            .concat(" ")
                                            .concat(destination));

                        } else { // if we do not have a destination but we do have a last point
                            llDestination.setVisibility(View.GONE); // we hide this part
                        }


                    } else {
                        llDestination.setVisibility(View.VISIBLE);
                        tvDestination.setText(getString(R.string.route_details_no_path)); // we show the default message
                        lvRouteDetailsList.setAdapter(null);
                    }

                    // setting up close button (does not depend on mayInteract)
                    ImageButton btnCloseDetails = vRouteDetails.findViewById(R.id.ibRDetailsClose);
                    btnCloseDetails.setOnClickListener(view -> popRouteDetails(false));

                    // setting up an empty onClickListener so the list doesn't close if the user clicks on it
                    vRouteDetails.setOnClickListener(view -> {
                    });
                }
            } else { // closing route details

                if (vRouteDetails.getVisibility() != View.GONE) { // we don't want to play the animation if this menu is already down
                    anim = AnimationUtils.loadAnimation(getContext(), R.anim.anim_bottom_out);
                    anim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            vRouteDetails.setVisibility(View.GONE);
                            mayInteract = true;
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                }
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "ERROR: popRouteDetails() failed to find view");
            e.printStackTrace();
        }

        if (vRouteDetails != null && anim != null) // preventing nullPointers
            vRouteDetails.startAnimation(anim);
    }

    /**
     * Translates a distance value to text so it can be presented on the layout
     * @param distance the distance to be translated in meters
     * @return the distance text to be presented on the layout. Eg: 5m or 2Km
     */
    @NonNull
    private String distanceToString(double distance) {
        String distText = ""; // the final distance string to be presented to the user

        Context c = getContext();

        if (distance > 0 && c != null) {
            // if not, we present the distance information
            String[] distInfo = String.valueOf(distance).split("\\.");

            String meters = distInfo[0]; // e.g: 39779.43821149336 to 39779

            if (meters.length() > 3) { // Km
                distText = meters.substring(0, meters.length() - 3).concat(c.getString(R.string.navbar_distance_suffix_kilometers));
            } else { // m
                distText = meters.concat(c.getString(R.string.navbar_distance_suffix_meters));
            }

            // taking out obsolete 0s at the beginning of the string
            distText = distText.replaceFirst("^0+(?!$)", ""); // 0015 to 15
        }

        return distText;
    }

    /* Camera Behaviour
    ----------------------------------------------- */
    /**
     * Focuses the user. The camera is animated depending on the current map mode
     */
    private void focusUser() {
        if (mapViewController.getCurrentMapMode() == MapViewController.MAP_MODE_OVERVIEW) {
            focusCameraOverview(navigationController.getUserPosition(), 1000, false);
        } else {
            focusCameraTurnByTurn();
        }
    }

    /**
     * Focuses the given point
     * @param point the point to focus on
     * @param speed the speed of the focus animation in milliseconds
     * @param zoomOutFirst should the camera animate and zoom out first? (only works in Overview mode)
     */
    @SuppressWarnings("SameParameterValue")
    private void focusCameraOverview(GeoPoint point, long speed, boolean zoomOutFirst) {

        // fetching the current map mode so we only fetch it once
        int mapMode = mapViewController.getCurrentMapMode();

        // zoom out first only works in Overview mode
        if (zoomOutFirst && mapMode == MapViewController.MAP_MODE_OVERVIEW) {
            // we zoom out a little first for half the animation duration
            MapPosition cameraPosition = new MapPosition();
            cameraPosition.setZoomLevel(NavConfig.ZOOM_OUT_ZOOM);
            cameraPosition.setPosition(mapView.map().getMapPosition().getGeoPoint());
            enableMapInteraction(false); // we disable map interaction to prevent weird behaviour
            if (mapView != null) {
                mapView.map().animator().animateTo((speed / 2), cameraPosition, Easing.Type.CUBIC_INOUT);
            } else {
                Log.w(TAG, "WARNING: Trying to focusCameraOverview() while mapView is null");
            }

            // we wait half the time and the zoom back again for half the animation duration
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                enableMapInteraction(true); // restoring map interaction
                focusCameraOverview(point, speed / 2, false);
            }, (speed / 2));
            return; // we don't want to continue the process
        }

        double zoom = mapView.map().getMapPosition().getScale(); // retrieving the current zoom
        MapPosition cameraPosition = new MapPosition();
        switch (mapMode) {
            case MapViewController.MAP_MODE_OVERVIEW:
                cameraPosition.setTilt(0f);
                if (zoom > NavConfig.DEFAULT_OVERVIEW_MAX_ZOOM) {
                    zoom = NavConfig.DEFAULT_OVERVIEW_MAX_ZOOM;
                } else if (zoom < NavConfig.DEFAULT_OVERVIEW_MIN_ZOOM) {
                    zoom = NavConfig.DEFAULT_OVERVIEW_MIN_ZOOM;
                }
                cameraPosition.setBearing(0);
                break;

            case MapViewController.MAP_MODE_TURN_BY_TURN:
                // clamping values. Camera scale for Turn by Turn must be between 131072.0 and 1048576.0 to maintain 3D effect
                if (zoom < NavConfig.DEFAULT_TBT_MIN_ZOOM || zoom > NavConfig.DEFAULT_TBT_MAX_ZOOM) {
                    zoom = NavConfig.DEFAULT_TBT_ZOOM; // defaultTbtZoom
                }

                cameraPosition.setTilt(61f); // 3D: 61f is the maximum tilt before displaying drawing bugs
                cameraPosition.setBearing(360 - navigationController.getUserBearing());
                break;
        }
        cameraPosition.setScale(zoom);
        cameraPosition.setPosition(point);

        if (mapView != null) {
            mapView.map().animator().animateTo(speed, cameraPosition, Easing.Type.CUBIC_INOUT);
        } else {
            Log.w(TAG, "WARNING: Trying to focusCameraOverview() while mapView is null");
        }
    }

    /**
     * Focuses the camera on the user arrow
     * NOTE: This only works in Turn by Turn mode
     */
    private void focusCameraTurnByTurn() {
        if (mapViewController.getCurrentMapMode() == MapViewController.MAP_MODE_TURN_BY_TURN) {

            enableMapInteraction(false);

            // setting viewport
            MapPosition cameraPosition = new MapPosition();
            cameraPosition.setTilt(61f); // 3D: 61f is the maximum tilt before displaying drawing bugs
            cameraPosition.setBearing(360 - navigationController.getUserBearing()); // inverting to counter-clockwise rotation
            double zoom = mapView.map().getMapPosition().getScale();
            if (zoom < 131072.0 || zoom > 1048576.0) { // if the user is too close or too far away
                zoom = NavConfig.DEFAULT_TBT_ZOOM; // we reset the zoom to the default value
            }
            cameraPosition.setScale(zoom);
            cameraPosition.setPosition(navigationController.getUserPosition());

            // playing the focus animation
            int animationTime = 1000;
            mapView.map().animator().animateTo(animationTime, cameraPosition, Easing.Type.LINEAR);

            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                // after the animation ends, we enable the user arrow
                updateUserArrowState();
                enableMapInteraction(true);
            }, animationTime);

        } else {
            Log.e(TAG, "ERROR: focusCameraTurnByTurn() is trying to run in Overview mode.");
        }
    }

    /**
     * Performs the turn-by-turn movement animation, if conditions are met
     * @param userPosition the user's position
     * @param userBearing the user's bearing
     */
    private void attemptTurnByTurnStep(GeoPoint userPosition, float userBearing) {
        // if the user is in turn-by-turn mode, we focus the user arrow
        if (focusCameraCounter <= 0 && mapViewController.getCurrentMapMode() == MapViewController.MAP_MODE_TURN_BY_TURN) {

            double currentZoom = mapView.map().getMapPosition().getScale(); // we keep the current zoom
            MapPosition cameraPosition = new MapPosition();
            cameraPosition.setTilt(61f); // 3D: 61f is the maximum tilt before displaying drawing bugs
            cameraPosition.setBearing( 360 - userBearing);
            cameraPosition.setScale(currentZoom);
            cameraPosition.setPosition(userPosition);

            mapView.map().animator().animateTo(NavConfig.GPS_CYCLE, cameraPosition, Easing.Type.LINEAR);

            // if the user arrow is not visible and we are supposed to show it
            if (llUserArrow.getVisibility() != View.VISIBLE && showArrowOnNextCycle) {
                final Handler handler = new Handler();
                handler.postDelayed(() -> { // we do so after the animation ends
                    /* if, and only if, the user is still in turn-by-turn mode (he could
                    have changed in the meantime) and we are still supposed to show the
                    arrow on the next cycle*/
                    if (mapViewController.getCurrentMapMode() == MapViewController.MAP_MODE_TURN_BY_TURN
                            && showArrowOnNextCycle) {
                        mapViewController.hideUserMarker();
                        llUserArrow.setVisibility(View.VISIBLE);
                    }
                }, NavConfig.GPS_CYCLE);
            }
        }
    }

    /**
     * Focuses the full route
     */
    private void focusRoute() {
        if (navigationController.hasPath()) {
            // retrieving the points from all path layers
            List<GeoPoint> geoPoints = new ArrayList<>();
            for (Layer l : mapView.map().layers()) {
                if (l.getClass() == PathLayer.class) {
                    PathLayer pathLayer = (PathLayer) l; // converting layer to PathLayer
                    geoPoints.addAll(pathLayer.getPoints()); // adding points
                }
            }

            // if we do have a route and we are in overview mode
            if (!geoPoints.isEmpty() && mapViewController.getCurrentMapMode() == MapViewController.MAP_MODE_OVERVIEW) {

                // then we generate a bounding box that encapsulates all route points
                BoundingBox focusBox = new BoundingBox(geoPoints);

                // we set the parameters for camera movement
                MapPosition focusParameters = new MapPosition();
                focusParameters.setByBoundingBox(
                        focusBox,
                        mapView.getWidth() - 300, // -300 to give some room to the focus box
                        mapView.getHeight() - 300);
                focusParameters.setTilt(0f); // 3D off
                focusParameters.setBearing(0f);

                // and we animate the camera
                mapView.map().animator().animateTo(1000, focusParameters, Easing.Type.CUBIC_INOUT);

            }
        }
    }

}
package com.dev.mytbt.Guide;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dev.mytbt.Routing.RoutePoint;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luís Henriques for MyTbt.
 * 21-03-2018, 10:11
 */

class NavPath {

    private static final String TAG = "tbtPath";

    @NonNull private List<RoutePoint> pathPoints; // this path's points
    private int lastPassedPointIndex = 0; // The index of the last point in this path that was passed by the user. It is used to prevent making calculations from scratch
    private RoutePoint myWaypoint;
    private String destinationAddress = "";

    // Path elements
    private List<NavPathLayer> layers;
    private List<NavPathMarker> pathMarkers;
    private List<RoutePoint> instructions;
    private List<RoutePoint> passedInstructions;

    /**
     * Creates a new NavPath instance
     */
    NavPath(@NonNull List<RoutePoint> points) {
        pathPoints = new ArrayList<>();

        layers = new ArrayList<>();
        pathMarkers = new ArrayList<>();
        instructions = new ArrayList<>();
        passedInstructions = new ArrayList<>();

        // the waypoint is assigned only when the path is created. It never changes
        if (!points.isEmpty()) {
            myWaypoint = points.get(points.size() -1);

        } else {
            // just guaranteeing that myWaypoint is never null! This should never happen
            Log.e(TAG, "ERROR when creating NavPath: the passed list of points is empty" );
            myWaypoint = new RoutePoint(new GeoPoint(0,0));
        }

        /* The new path should have at least 3 points.
        - The point that was artificially added behind the user when the path was received (point 0)
        - The starting point, or the first point on the path (point 1)
        - The destination point (point points -1) */
        if (points.size() >= 3) {
            /* when creating a new path, we force the first point to have a START icon.
            Point 0 is always ignored, because it is the point that was artificially
            created when receiving the path from a routing API */
            points.get(1).setIcon(RoutePoint.DirIcon.START);
            points.get(1).setMarker(RoutePoint.MarkerType.START);

            // the last element of the list is always the destination
            points.get(points.size() - 1).setWaypoint(true);
            points.get(points.size() - 1).setIcon(RoutePoint.DirIcon.DESTINATION);
            points.get(points.size() - 1).setMarker(RoutePoint.MarkerType.DESTINATION); // this overrides any marker set before

            pathPoints = points;

            // now that all essential path points are defined, we start building some elements
            layers = buildLayersAndMarkers(points);
            instructions = buildInstructions(points);

            destinationAddress = myWaypoint.getStreetName();
        }
    }

    /**
     * Adds a group of points to the path
     * @param newPoints the list of points to be added
     */
    void addPoints(List<RoutePoint> newPoints) {

        if (newPoints != null && newPoints.size() > 1) {
            /* we force the first point in our adjusted path to have a READJUSTED PATH icon.
            As usual, point 0 is behind the user and is ignored */
            newPoints.get(1).setIcon(RoutePoint.DirIcon.READJUSTED);
            newPoints.get(1).setMarker(RoutePoint.MarkerType.READJUSTED);

            lastPassedPointIndex = 0; // we reset the index of the last passed point

            pathPoints.addAll(0, newPoints); // we add the new points to the current path's points

            layers = buildLayersAndMarkers(pathPoints); // then we override and recreate the layers and markers as well
            instructions = buildInstructions(pathPoints); // we also override and recreate the instruction points

        } else {
            Log.e(TAG, "ERROR: addPoints() is trying to add an empty list of points");
        }
    }

    /**
     * Deletes all points from the current route up to the specified point
     * @param point the point up to which all points are to be removed (exclusive)
     */
    void discardPointsUpTo(RoutePoint point) {

        /* since we'll simply redraw everything from scratch when adjusting a path, it makes no sense to manage
        which layer points should be removed or not, nor which instructions and markers we want to keep. We'll simply
        delete them all when updating this path. So, here we'll just manage the RoutePoints */

        // discarding points
        if (pathPoints.contains(point)) { // if our path contains the point

            List<RoutePoint> obsoleteRoutePoints = new ArrayList<>();

            // we remove each point from our path up to the specified point
            for (RoutePoint rp : pathPoints) {
                if (rp.equals(point) || rp.isWaypoint()) { // we never get past the waypoint though
                    break;
                } else {
                    obsoleteRoutePoints.add(rp);
                }
            }
            pathPoints.removeAll(obsoleteRoutePoints);

        } else {
            Log.e(TAG, "ERROR: discardPointsUpTo() is trying to remove a non existing point from the path.");
        }
    }

    /**
     * Retrieves the relevant decision points (instruction points) from a list of RoutePoints
     * @param routePoints the list of RoutePoints
     * @return a list with the main instruction points for the current path
     */
    private List<RoutePoint> buildInstructions(List<RoutePoint> routePoints) {

        Log.d(TAG, "Building instructions for the new path...");
        List<RoutePoint> newInstructions = new ArrayList<>();

        /* This function simply filters which RoutePoints should not be used as instructions.
        The remaining points are added to the list of instructions */
        if (routePoints.size() > 0) {
            /* our criteria for defining the next instruction point is any change
            in the instruction text or icon. We ignore street name changes, as they are irrelevant */
            for (RoutePoint rp : routePoints) {

                // if this is the first point
                if (rp.getIcon().equals(RoutePoint.DirIcon.START)) {
                    /* we clear all instructions before it.
                    Note: This prevents having instructions before the starting point */
                    newInstructions.clear();
                }

                // we ignore points with no instruction value
                if (rp.getIcon().equals(RoutePoint.DirIcon.UNKNOWN)
                        || rp.getIcon().equals(RoutePoint.DirIcon.CONTINUE)) {
                    // NOTE: We decided to ignore "road name change" instructions
                    //&& (rp.getStreetName().isEmpty() || rp.getStreetName().equals(lastInstructionPoint.getStreetName())) // empty or identical street name
                    //&& rp.getInstruction().isEmpty()) { //  this is supposed to ignore road name change instructions
                    continue; // go to the next item on the list
                }

                /* NOTE: With our offline alternative, many points repeat the same instruction icon,
                so we have to check if the given instructions are pertinent or not. To do so, we simply
                check for the traffic level of each points. If it is -1 (offline), we only accept
                points with instruction text.*/
                if (rp.getTrafficLevel() < 0
                        && rp.getInstruction().isEmpty()
                        && !rp.getIcon().equals(RoutePoint.DirIcon.START)
                        && !rp.getIcon().equals(RoutePoint.DirIcon.READJUSTED)
                        && !rp.getIcon().equals(RoutePoint.DirIcon.DESTINATION)) {
                    continue;
                }

                newInstructions.add(rp); // we add it to the list
                Log.d(TAG, "New Instruction Point added -> " + rp.getIcon().toString() + " on " + rp.getStreetName() + ". The original text is: " + rp.getInstruction());

            }
        } else {
            Log.e(TAG, "ERROR: can't build instructions for a 0 point list");
        }
        return newInstructions;
    }

    /**
     * Build path layers from a list of RoutePoints. If there is no main layer, creates one. If there is one, updates it but doesn't return it.
     *
     * @param routePoints the list of RoutePoints from which we create the layers
     * @return a list of all the just created path layers
     */
    @NonNull
    private List<NavPathLayer> buildLayersAndMarkers(List<RoutePoint> routePoints) {
        Log.d(TAG, "Generating new path layers...");

        /* This system will always connect the points in the layers. As far as I know, there is no way around it
        In order to draw line colors depending on traffic, we have to draw many layers with lines of 2 points each.
        As this approach may cause performance issues, and because the no-traffic lines are the most common, we will
        always have one, and only one, main no-traffic layer per path. This is the mainPathLayer instance.
        Only the other traffic conditions will create a 2 point layer. */

        List<NavPathLayer> pathLayers = new ArrayList<>(); // the list of all layers for the current path. To be returned by this func
        List<GeoPoint> baseLayerPoints = new ArrayList<>(); // the full list of points for the current path

        /* we reset all info markers. NOTE: Having this here is not ideal, but it prevents unnecessary processing.
        The alternative would be to create a separate buildMarkers() function that would have to loop through all
        RoutePoints as well. */
        pathMarkers = new ArrayList<>();

        // for each one of the received RoutePoints (ignoring point 0, which is the point behind the user)
        for (int i = 1; i < routePoints.size(); i++) {
            RoutePoint rp = routePoints.get(i);

            // first, we always add the current point to the path's base layer
            GeoPoint p = rp.getGeoPoint();
            baseLayerPoints.add(p);

            /* if the point is from a path that was calculated online,
            which means that it may have appropriate traffic info */
            if (rp.getTrafficLevel() >= 0) {

                /* Traffic lines are generated from the current rp to the next rp, so
                we don't want to check the last point, which has no next rp */
                if (i != routePoints.size() - 1) { // if the current point is not the last point

                    /* we check the traffic level of the next point, so we know if we should draw a colored
                    line from the current point to the next one */
                    int nextTrafficLevel = routePoints.get(i + 1).getTrafficLevel();

                    // if there is traffic above 0 on the next point
                    if (nextTrafficLevel > 0) {
                        // we create a layer containing a single colored line, from the current rp to the next rp
                        ArrayList<GeoPoint> trafficLayerPoints = new ArrayList<>();
                        trafficLayerPoints.add(rp.getGeoPoint());
                        trafficLayerPoints.add(routePoints.get(i + 1).getGeoPoint());

                        // and add it to the list
                        NavPathLayer trafficLayer = new NavPathLayer(nextTrafficLevel, trafficLayerPoints);
                        pathLayers.add(trafficLayer);

                        // we also set traffic markers as appropriate, since they are not set by the routing APIs
                        if (nextTrafficLevel > 2) {
                            rp.setMarker(RoutePoint.MarkerType.MASS_TRANSIT);
                        } else {
                            rp.setMarker(RoutePoint.MarkerType.TRAFFIC);
                        }

                        Log.d(TAG, "New traffic layer added with traffic level: " + nextTrafficLevel);
                    }
                }
            } // so now our list of layers "pathLayers" contains a list of 2 lined traffic layers

            /* if the current RoutePoint has an associated marker, we add the respective
            marker to our list (including destinationAddress) */
            RoutePoint.MarkerType rpMk = rp.getMarker();
            if (!rpMk.equals(RoutePoint.MarkerType.NONE)) {
                NavPathMarker mk = new NavPathMarker(rpMk, p);
                pathMarkers.add(mk);
                Log.w(TAG, "New marker added to the path -> " + rp.getMarker());
            }
        }
        // finally, we create the main path layer and add it at index 0, so it is drawn under all other layers
        NavPathLayer mainPathLayer = new NavPathLayer(0, baseLayerPoints);
        pathLayers.add(0, mainPathLayer);

        Log.d(TAG, "Path layers complete. Created " + pathLayers.size() + " layers.");
        return pathLayers;
    }

    /**
     * Sets the index of the latest point that was detected behind the user when he was on the path
     *
     * @param lastPointBehindUser the point that was detected behind the user
     * @return returns true if the point index was updated. Returns false if the index remained the same
     */
    boolean progressOnPath(RoutePoint lastPointBehindUser) {
        int newIndex = pathPoints.indexOf(lastPointBehindUser);

        if (newIndex <= lastPassedPointIndex || !pathPoints.contains(lastPointBehindUser)) {
            return false;
        } else {
            lastPassedPointIndex = newIndex; // we update the index of the last passed point
            updateInstructions(); // we update the instructions. This must come AFTER updating the LastPointIndex
            return true;
        }
    }

    /**
     * Updates the instructions up to the lastPassedPointIndex
     */
    private void updateInstructions() {

        List<RoutePoint> passedPoints = pathPoints.subList(0, lastPassedPointIndex + 1);

        // we create a list of the recent passed instructions
        List<RoutePoint> obsoleteInstructions = new ArrayList<>(instructions);
        obsoleteInstructions.retainAll(passedPoints);

        // then we update both instructions and passed instructions lists
        passedInstructions.addAll(obsoleteInstructions);
        instructions.removeAll(passedPoints);
    }

    // Gets
    /**
     * @return Returns all the points in this path
     */
    @NonNull
    List<RoutePoint> getPathPoints() {
        return pathPoints;
    }

    /**
     * @return returns the index of the last point in this path that was passed by the user.
     * To be considered a passed point, it must have been, at some point behind the user.
     * Never in front of him.
     */
    int getLastPassedPointIndex() {
        return lastPassedPointIndex;
    }

    /**
     * @return Returns whether or not this path has at least one more point after the point that was
     * last passed by the user. In other words, is there at least one more point in front of the user?
     */
    private boolean hasANextPoint() {
        return getPathPoints().size() > getLastPassedPointIndex() + 1;
    }

    /**
     * @return Returns the next point in the path, which is in front of the user, if there is one
     */
    @Nullable
    RoutePoint getNextPoint() {
        if (hasANextPoint()) {
            return getPathPoints().get(getLastPassedPointIndex() + 1);
        } else {
            return null;
        }
    }

    @NonNull
    RoutePoint getWaypoint() {
        return myWaypoint;
    }

    /**
     * @return returns the destinationAddress's street name
     */
    String getDestinationName() {
        return destinationAddress;
    }

    /**
     * @return returns the next instruction, if there is one
     */
    @Nullable
    RoutePoint getNextInstruction() {
        if (!instructions.isEmpty()) {
            return instructions.get(0);
        } else {
            return null;
        }
    }

    @NonNull
    List<RoutePoint> getInstructionPoints() {
        if (instructions == null) {
            return new ArrayList<>();
        }
        return instructions;
    }

    @NonNull
    List<RoutePoint> getPassedInstructionPoints() {
        if (passedInstructions == null) {
            return new ArrayList<>();
        }
        return passedInstructions;
    }


    @NonNull
    List<NavPathLayer> getPathLayers() {
        if (layers == null) {
            return new ArrayList<>();
        }
        return layers;
    }

    @NonNull
    List<NavPathMarker> getPathMarkers() {
        if (pathMarkers == null) {
            return new ArrayList<>();
        }

        return pathMarkers;
    }

}

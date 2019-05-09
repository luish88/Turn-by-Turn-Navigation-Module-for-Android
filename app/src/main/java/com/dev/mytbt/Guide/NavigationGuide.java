package com.dev.mytbt.Guide;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.util.Log;

import com.dev.mytbt.NavConfig;
import com.dev.mytbt.R;
import com.dev.mytbt.Routing.RoutePoint;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Luís Henriques for MyTbt.
 * 28-02-2018, 12:23
 *
 * This class is responsible for processing and (dis)playing instructions, whether they are
 * voiced, written, or icons
 */
public class NavigationGuide {

    private static final String TAG = "tbtGuide";

    private static TextToSpeech tts;
    private static boolean ttsIsRunning = false;
    private static boolean ttsMaySpeak = false;

    public enum InstructionType {NEARBY, FAR_AWAY}
    private List<String> playQueue = new ArrayList<>(); // NOTE: we created our own queue so we have a maximum of 3 messages in ir

    // last played instructions
    private RoutePoint lastPlayedNearbyInstruction = null;
    private RoutePoint lastPlayedFarAwayInstruction = null;
    private String lasPlayedText = "";

    public NavigationGuide(Context c) {
        initializeTextToSpeech(c);
    }


    // Text-to-Speech service
    private void initializeTextToSpeech(Context c) {
        Log.w(TAG, "Initializing text to speech...");
        final Locale language = NavConfig.getLanguage();

        /* NOTE: if the user pauses the app before the text-to-speech service has time to begin,
        it causes a memory leak. There is no way to prevent it. It shouldn't matter, though, since
        the app is closed by that point and its memory is freed by the OS */

        ttsIsRunning = true;
        tts = new TextToSpeech(c, status -> {
            if (status == TextToSpeech.SUCCESS){
                int langResult = tts.setLanguage(language);
                if (langResult == TextToSpeech.LANG_MISSING_DATA
                        || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "ERROR: Text to speech language is not supported");
                    ttsIsRunning = false;
                } else {
                    ttsMaySpeak = true;
                    Log.e(TAG, "...Text to Speech ready!");

                    // playing queue on resuming app
                    if (!playQueue.isEmpty()) {
                        resumeAndPlay(c);
                    }
                }
            }else{
                Log.e(TAG, "ERROR: Text to Speech initialization failed");
                ttsIsRunning = false;
            }
        });
    }

    /**
     * Plays the queued messages in order
     * @param c the current Context object
     */
    void resumeAndPlay(Context c) {
        if (!ttsIsRunning || tts == null) {
            initializeTextToSpeech(c);
        } else {
            if (ttsMaySpeak && !tts.isSpeaking() && !playQueue.isEmpty()) {
                tts.speak(playQueue.get(0), TextToSpeech.QUEUE_FLUSH, null);
                playQueue.remove(0);
            }
        }
    }

    /**
     * Closes the Text to Voice Service to prevent memory leaks after onPause()
     */
    void closeTextToSpeechService(){
        // set this up first in case of memory leaking (onPause before the system had time to set up the text-to-speech)
        if (ttsIsRunning && tts != null) {
            tts.stop();
            tts.shutdown();
        }
        ttsMaySpeak = false;
        ttsIsRunning = false;
    }

    // Play queue management
    /**
     * Adds a string to the current queue
     * @param instructionText the instruction text to be added to the queue
     */
    private void addToPlayQueue(String instructionText) {
            while (playQueue.size() > 2) { // if we have 3 items in our queue
                playQueue.remove(0); // we clear the oldest instructions (the ones at the beginning)
            }
            playQueue.add(instructionText); // and add our new instruction at the bottom
    }

    /**
     * Clears the current queue
     */
    private void clearQueue() {
        playQueue.clear();
    }


    // Navigation events
    /**
     * Speaks the instructions to the user. Called when the user is found on the path
     */
    void onUserFoundOnPath(NavPath path, GeoPoint userPosOnPath, double distToNextInstruction, Context c) {

        if (path == null) {
            return;
        }

        // first, we trigger voice instructions for our immediate point, which is the next point in our path
        RoutePoint nextPoint = path.getNextPoint();
        if (nextPoint != null && path.getInstructionPoints().contains(nextPoint)) { // if our next point is an instruction point

            // first we play the instructions for the immediate point
            if (userPosOnPath.sphericalDistance(nextPoint.getGeoPoint()) < NavConfig.WARM_RADIUS // and the user is close enough
                    && !nextPoint.equals(lastPlayedNearbyInstruction)) { // and it is different than the last played instruction

                String instruction = getInstructionSpeech(nextPoint, InstructionType.NEARBY, 0, c);

                // once we have a nearby instruction, we play it immediately, and flush the tts queue
                if (!instruction.isEmpty() && ttsMaySpeak) {
                    tts.speak(instruction,TextToSpeech.QUEUE_FLUSH, null);
                }

                lastPlayedNearbyInstruction = nextPoint; // preventing repeated messages

                Log.d(TAG, "Nearby instruction -> " + instruction);
            }
        }

        /* after giving priority to the immediate instructions, we play the instructions for the next instruction point.
        This is done with the help of a queue, so all messages are given in the proper order. We also do not store more than
        3 messages at once, or it would become overwhelming and the timing wouldn't be appropriate, as we would be
        waiting for many previous messages to play the current, most appropriate, one*/
        RoutePoint nextInstructionPoint = path.getNextInstruction();
        if (nextInstructionPoint != null && !nextInstructionPoint.equals(lastPlayedFarAwayInstruction) && distToNextInstruction > 0) { // when we have a new far away instruction

            String instruction = getInstructionSpeech(nextInstructionPoint, InstructionType.FAR_AWAY, distToNextInstruction, c);

            Log.d(TAG, "Distant instruction -> " + instruction);

            addToPlayQueue(instruction);

            lastPlayedFarAwayInstruction = nextInstructionPoint; // preventing repeated messages
        }

        resumeAndPlay(c); // if the tts isn't speaking, we play the queue
    }

    /**
     * When the user is not found on the path
     */
    void onUserNotFoundOnPath() {
        clearQueue(); // we clear the playing queue
    }

    /**
     * Plays voice instructions for beginning a new path
     */
    void onUserStartingNewPath(NavPath currentPath, Context c) {

        clearQueue();

        if (currentPath == null) {
            return;
        }

        String voiceMessage = translateSign(RoutePoint.DirIcon.START, InstructionType.NEARBY, c);

        if (!currentPath.getDestinationName().isEmpty()) {
            voiceMessage = voiceMessage
                    .concat(" ")
                    .concat(c.getString(R.string.voice_start_destination_prefix))
                    .concat(" ")
                    .concat(currentPath.getDestinationName());
        }

        addToPlayQueue(voiceMessage);
        resumeAndPlay(c);

        Log.d(TAG, "New path instruction -> " + voiceMessage);
    }

    /**
     * Plays voice instructions for readjusting a path
     */
    void onUserReadjustedPath(Context c){
        clearQueue();
        String voiceMessage = translateSign(RoutePoint.DirIcon.READJUSTED, InstructionType.NEARBY, c);
        addToPlayQueue(voiceMessage);
        resumeAndPlay(c);

        Log.d(TAG, "Readjusted path -> " + voiceMessage);
    }

    /**
     * Plays voice instructions for when the user reaches his destination
     */
    void onUserReachedDestination(Context c) {
        clearQueue();
        String voiceMessage = c.getString(R.string.voice_destination_immediate);
        addToPlayQueue(voiceMessage);
        resumeAndPlay(c);

        Log.d(TAG, "Reached destination -> " + voiceMessage);
    }

    /**
     * Plays voice instructions for when there is an error while calculating a new route
     */
    void onFailingNewRoute(Context c) {

        Log.e(TAG, "onFailingRouteUpdate: --------------------------------- " );

        clearQueue();
        String voiceMessage = c.getString(R.string.voice_error_new_route);
        addToPlayQueue(voiceMessage);
        resumeAndPlay(c);
    }

    /**
     * Plays voice instructions for when there is an error while updating the current route
     */
    void onFailingRouteUpdate(Context c) {
        clearQueue();
        String voiceMessage = c.getString(R.string.voice_error_updating_route);
        addToPlayQueue(voiceMessage);
        resumeAndPlay(c);
    }

    // Instruction building
    /**
     * Builds the speech text from the instructions
     * @param instructionPoint the point from which we'll build the instruction text
     * @return the instruction text, to be read by the speech to text
     */
    private String getInstructionSpeech(RoutePoint instructionPoint, InstructionType type, double distance, Context c) {

        // now we'll build the text message depending on what we find
        String voiceMessage = "";
        String justDistanceStr = ""; // a string containing distance information only

        /* Non-standard conditions, in which we do not build instructions */
        if (instructionPoint.getIcon() != RoutePoint.DirIcon.DESTINATION
                && instructionPoint.getIcon() != RoutePoint.DirIcon.START
                && instructionPoint.getIcon() != RoutePoint.DirIcon.READJUSTED) {
            // we only talk about distances if we are referring to a far away instruction
            if (type == InstructionType.FAR_AWAY) {

                // if the next instruction is very near, we won't add the distance
                if (distance > 50) { // so, if the instruction is NOT too close (under 50 meters)

                    // we add the prefix
                    String distPrefix = c.getString(R.string.voice_distance_prefix).concat(" ");

                    String[] distStr = String.valueOf(distance).split("\\.");
                    String baseDist = distStr[0];

                    // final string elements
                    String finalDist;
                    String metric;
                    String andAHalf = ""; // literally "and a half", if needed (in case of kilometers)

                    if (baseDist.length() > 3) { // kilometers

                        try {
                            // trying to find if the remaining distance is >= 500m
                            double meters = Double.valueOf(baseDist.substring(baseDist.length() - 3));

                            if (meters >= 500) { // if so, we add the "and a half" text
                                andAHalf = c.getString(R.string.voice_distance_metric_and_a_half);
                            }


                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        finalDist = baseDist.substring(0, baseDist.length() - 3); // 1500 to 15

                        metric = c.getString(R.string.voice_distance_metric_kilometers); // metric is always "kilometers" (plural) in case parsing fails

                        try {

                            double km = Double.valueOf(finalDist);

                            if (km < 2) { // single kilometer
                                metric = c.getString(R.string.voice_distance_metric_single_kilometer);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else { // meters
                        finalDist = baseDist; // 366 = 366 meters

                        // we clear the last number of the distance, so the voice isn't too specific. E.g: 231 = 230 & 23 = 20
                        finalDist = finalDist.substring(0, finalDist.length() - 1);
                        finalDist = finalDist.concat("0");

                        metric = c.getString(R.string.voice_distance_metric_meters);

                    }

                    // we take out any 0s before
                    finalDist = finalDist.replaceFirst("^0+(?!$)", "");

                    // building the distance string. Note: the last ", " must be here, so we can compare string lengths by the end
                    voiceMessage += distPrefix
                            .concat(" ")
                            .concat(finalDist)
                            .concat(" ")
                            .concat(metric)
                            .concat(" ")
                            .concat(andAHalf)
                            .concat(", ");

                    // we keep the string containing just distance, so we ignore the whole message if no further content is added
                    justDistanceStr = voiceMessage;
                }

                // up to this point, if we are not referring to an immediate point, we have a valid distance sentence
            }

            /* now we'll building the instruction itself according to the information we have
            we'll prioritize the already built-in instruction, but most times there is none */
            if (!instructionPoint.getInstruction().isEmpty() || !instructionPoint.getInstruction().equals("")) { // if we have a voice instruction
                voiceMessage += instructionPoint.getInstruction(); // we simply use it

            } else { // if we do not have a built in instruction, we'll use the sign as a guide for our instruction
                if(instructionPoint.getIcon() != RoutePoint.DirIcon.ROUNDABOUT) {
                    voiceMessage += translateSign(instructionPoint.getIcon(), type, c); // we translate the sign as appropriate
                } else {
                    // as for roundabouts, we want to treat them differently
                    if (instructionPoint.getRoundaboutExit() > 0) { // if there is a set roundabout exit
                        if (type.equals(InstructionType.FAR_AWAY)) {
                            voiceMessage += " "
                                    .concat(c.getString(R.string.voice_enter_roundabout))
                                    .concat(", ");
                        }
                        // if it is a nearby instruction, we only want the text "take the 2nd exit" without the "at the roundabout" part
                        voiceMessage += " "
                                .concat(c.getString(R.string.voice_enter_roundabout_with_exit))
                                .concat(" ")
                                .concat(String.valueOf(instructionPoint.getRoundaboutExit()))
                                .concat(" ")
                                .concat(c.getString(R.string.voice_enter_roundabout_exit_suffix))
                                .concat(". ");
                    }

                    // NOTE: We ignore roundabout instructions without exit

                }
            }

            /* up to this point, we have a full instruction that may or may not have a distance. Some examples:
            e.g: in about 500 meters, at the roundabout, take the second exit <- distant instruction with built-in instruction text
            e.g: in about 230, turn right <- distant instruction with translated sign
            e.g: turn left <- nearby instruction
            e.g: in about 1 and a half kilometers, you will enter <- distant instruction with road name change
            e.g: you just entered <- nearby instruction with road name change
            e.g: in about 200 meters, <- distant instruction with UNKNOWN type (that's why we keep justDistanceStr) */

            /* finally, we compare the street name of our instruction point with the previously detected instruction point
            Note: we haven't updated nextInstructionPoint yet. We can still compare it with the current one */
            if (!instructionPoint.getStreetName().isEmpty()) { // so, if we have a street name

                // if the street name wasn't already mentioned in the built-in instruction
                if (!instructionPoint.getInstruction().contains(instructionPoint.getStreetName())

                    // nor in the previous nearby instructions
                    && (lastPlayedNearbyInstruction == null
                    || !instructionPoint.getStreetName().equals(lastPlayedNearbyInstruction.getStreetName()))

                    // and this is a nearby instruction
                    && (/*type == InstructionType.NEARBY // NOTE: taken out

                    // or, in case it is a distant instruction, if we don't repeat the street name
                    ||*/ (type == InstructionType.FAR_AWAY
                    && (lastPlayedFarAwayInstruction == null
                    || !instructionPoint.getStreetName().equals(lastPlayedFarAwayInstruction.getStreetName()))))) {

                        // we add the prefix, if the message doesn't have one yet
                        if (!voiceMessage.contains(c.getString(R.string.voice_road_name_change_distant))
                                && !voiceMessage.contains(c.getString(R.string.voice_road_name_change_immediate))) {
                            voiceMessage += ", "
                                    .concat(c.getString(R.string.voice_road_name_change_distant));
                        }
                        voiceMessage += " ".concat(instructionPoint.getStreetName()); // and we finally add the street name
                }
            }
        }
        // finally, we simply correct the instruction
        Orthographer orthographer = new Orthographer(NavConfig.getLanguage());
        voiceMessage = orthographer.correctText(voiceMessage);
        voiceMessage = voiceMessage.replaceAll(" , ", ", ");

        /* let's do some checking so we don't return unwanted messages. We'll ignore messages that:
        - contain only distance information
        - are exactly the same as the last played message */
        if (voiceMessage.equals(justDistanceStr) || voiceMessage.equals(lasPlayedText)) {
            Log.w(TAG, "Ignored instruction -> " + voiceMessage );
            return "";
        }
        lasPlayedText = voiceMessage;
        return voiceMessage;
    }

    /**
     * Translates an icon to text to be read by the text to speech
     * @param icon the icon from RoutePoint.DirIcon
     * @return the text to be read
     */
    @NonNull
    private String translateSign(RoutePoint.DirIcon icon, InstructionType type, Context c) {
        switch (icon) {
            case START: return c.getString(R.string.voice_start);
            case READJUSTED: return c.getString(R.string.voice_readjusted);
            case CONTINUE: return c.getString(R.string.voice_continue);
            case DESTINATION:
                if (type == InstructionType.NEARBY) {
                    return ""; // this instruction is ignored, since we will force it from the MapMaster instance
                } else {
                    return c.getString(R.string.voice_destination_distant);
                }
            case TURN_SLIGHT_LEFT: return c.getString(R.string.voice_turn_slight_left);
            case TURN_SLIGHT_RIGHT: return c.getString(R.string.voice_turn_slight_right);
            case TURN_LEFT: return c.getString(R.string.voice_turn_left);
            case TURN_RIGHT: return c.getString(R.string.voice_turn_right);
            case TURN_SHARP_LEFT: return c.getString(R.string.voice_turn_sharp_left);
            case TURN_SHARP_RIGHT: return c.getString(R.string.voice_turn_sharp_right);
            case KEEP_LEFT: return c.getString(R.string.voice_keep_left);
            case KEEP_RIGHT: return c.getString(R.string.voice_keep_right);
            case ON_RAMP_LEFT:return c.getString(R.string.voice_enter_ramp_left);
            case ON_RAMP_RIGHT:return c.getString(R.string.voice_enter_ramp_right);
            case OFF_RAMP_LEFT:return c.getString(R.string.voice_exit_ramp_left);
            case OFF_RAMP_RIGHT:return c.getString(R.string.voice_exit_ramp_right);
            case ROUNDABOUT: return c.getString(R.string.voice_enter_roundabout);
            case LEAVE_ROUNDABOUT: return c.getString(R.string.voice_leave_roundabout);
            case ROAD_NAME_CHANGE: return ""; // we decided to ignore road name change instructions
            case U_TURN: return c.getString(R.string.voice_u_turn);
            default: // UNKNOWN and others
                return "";
        }
    }
}

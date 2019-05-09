package com.dev.mytbt.Tools;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.dev.mytbt.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luís Henriques for MyTbt.
 * 14-09-2018, 23:29
 */
public class PermissionManager {

    private static final String TAG = "tbtPermissionManager"; // Debug tag

    private static boolean hasAllPermissions = false;
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1; // permission request code

    // List here all the required permissions (must also be stated in the manifest):
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            // Internet
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,

            // GPS location
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,

            // Reading and writing files
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    // Callback for after granting all permissions
    public interface PermissionsGrantedCallback {
        void onPermissionsGranted();
        void onPermissionsNotGranted();
        void onAbsoluteRefuse();
    }

    /**
     * Checks all required permissions. In case there are any missing permissions, shows a popup message explaining
     * why the permissions are needed.
     * Simply call it in your activity's onResume() with:
     * PermissionManager.checkPermissions(this);
     * NOTE: Only run code that requires permissions after calling this onResume()
     * @param currentActivity the current activity. Probably the MainActivity.
     * @param callback a PermissionsGrantedCallback to add behaviour after all permissions are granted or in case of an absolute refuse
     */
    public static void requestPermissions (@NonNull Activity currentActivity, PermissionsGrantedCallback callback) {

        // we automatically grant permissions is SDK version is below marshmallow
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.w(TAG, "Android version is below 6.0. All permissions are automatically granted.");
            hasAllPermissions = true;
            callback.onPermissionsGranted();

        } else { // else, we request permissions
            checkPermissions(currentActivity, callback);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static void checkPermissions(@NonNull Activity currentActivity, PermissionsGrantedCallback callback) {

        List<String> missingPermissions = getMissingPermissions(currentActivity);

        if (missingPermissions.isEmpty()) { // if the user grants all permissions
            hasAllPermissions = true;
            Log.w(TAG, "...All permissions granted.");
            callback.onPermissionsGranted();

        } else { // if the user does not grant all permissions
            hasAllPermissions = false;
            callback.onPermissionsNotGranted();
            popPermissionInfo(currentActivity, missingPermissions, callback); // we pop the permission request
        }
    }

    private static List<String> getMissingPermissions(@NonNull Activity currentActivity) {

        Log.w(TAG, "Checking permissions...");
        List<String> missingPermissions = new ArrayList<>();

        // first, we check all required permissions
        for (final String permission : REQUIRED_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(currentActivity, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
                Log.e(TAG, "...missing permission: " + permission);
            }
        }
        // return whether there are missing permissions
        return missingPermissions;
    }

    private static void popPermissionInfo(@NonNull Activity currentActivity, List<String> missingPermissions, final PermissionsGrantedCallback callback) {

        AlertDialog dialog;

        // converting to final
        final Activity finalActivity = currentActivity;
        final String[] permissions = missingPermissions.toArray(new String[missingPermissions.size()]);

        // listener for the dialog buttons
        AlertDialog.OnClickListener buttonListener = (dialog1, which) -> {

            if (dialog1 != null) {
                dialog1.dismiss(); // immediately closing the dialog onClick
            }

            if (which == AlertDialog.BUTTON_POSITIVE) {
                // request all missing permissions
                ActivityCompat.requestPermissions(finalActivity, permissions, REQUEST_CODE_ASK_PERMISSIONS);

            } else if (which == AlertDialog.BUTTON_NEGATIVE) {
                callback.onAbsoluteRefuse(); // if the user refuses to grant permissions, we call this method on the callback
            }
        };

        // Building the dialog text depending on the missing permissions
        String dialogText = currentActivity.getString(R.string.permissionPromptMainText);
        if (missingPermissions.contains(Manifest.permission.INTERNET)) {
            dialogText = dialogText.concat("\n".concat(currentActivity.getString(R.string.permissionPromptInternet)));
        }
        if (missingPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
            dialogText = dialogText.concat("\n".concat(currentActivity.getString(R.string.permissionPromptLocation)));
        }
        if (missingPermissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            dialogText = dialogText.concat("\n".concat(currentActivity.getString(R.string.permissionPromptReadWrite)));
        }

        // displaying the dialog
        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(currentActivity);
        dialogBuilder.setTitle(currentActivity.getString(R.string.permissionPromptTitle));
        dialogBuilder.setMessage(dialogText);
        dialogBuilder.setPositiveButton(currentActivity.getString(R.string.genericOk), buttonListener);
        dialogBuilder.setNegativeButton(currentActivity.getString(R.string.genericCancel), buttonListener);
        dialogBuilder.setCancelable(false);
        dialog = dialogBuilder.create();
        dialog.show();
    }

    /**
     * @return Returns whether the system has all required permissions or not
     */
    public static boolean hasAllPermissions() {
        return hasAllPermissions;
    }
}

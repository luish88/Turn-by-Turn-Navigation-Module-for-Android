package com.dev.mytbt.Tools;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.dev.mytbt.NavConfig;

public class GpsService extends Service {

    static final String TAG = "tbtGpsService"; // Debug tag

    private LocationListener myLocationListener;
    private LocationManager locationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        myLocationListener = new LocationListener() { // GPS listener configuration
            @Override
            public void onLocationChanged(Location location) {

                // we only send data if the provider is the GPS provider. This prevents provider interference.
                if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                    Intent gpsData = new Intent("gps_location_update");
                    gpsData.putExtra("lat", location.getLatitude());
                    gpsData.putExtra("long", location.getLongitude());
                    gpsData.putExtra("bearing", location.getBearing());
                    gpsData.putExtra("accuracy", location.getAccuracy());
                    sendBroadcast(gpsData); // broadcasts the intent to be received by any class that wants to receive GPS data (MainActivity ?)
                }
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.w(TAG, "GPS service onStatusChanged()");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.w(TAG, "GPS provider enabled");

                Intent gpsData = new Intent("gps_status_changed");
                gpsData.putExtra("enabled", true);
                sendBroadcast(gpsData);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.w(TAG, "GPS provider disabled");

                Intent gpsData = new Intent("gps_status_changed");
                gpsData.putExtra("enabled", false);
                sendBroadcast(gpsData);
            }
        };
        runGpsSetup(); // Starts the GPS, checks permissions and the timer to receive data every X seconds
    }

    @Override
    public void onDestroy() { // to extend battery life, we stop using the listener when the app is closed
        if(locationManager != null) {
            locationManager.removeUpdates(myLocationListener);
        }
        super.onDestroy();
    }

    private void runGpsSetup() { // Starts the GPS, checks permissions and the timer to receive data every X seconds
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        if (locationManager != null) {
            Criteria c = new Criteria();
            c.setAccuracy(Criteria.ACCURACY_FINE);
            c.setPowerRequirement(Criteria.POWER_LOW);
            String provider = locationManager.getBestProvider(c, true);
            locationManager.requestLocationUpdates(provider, NavConfig.GPS_CYCLE, 0, myLocationListener);
        } else {
            Log.e(TAG, "ERROR: LocationManager is null!");
        }
    }

    /**
     * Checks if this service is is running
     * @param currentActivity the current Activity instance
     * @return whether the GPS Service is running or not
     */
    public static boolean isRunning(Activity currentActivity) {
        if (currentActivity != null) {
            ActivityManager manager = (ActivityManager) currentActivity.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (GpsService.class.getName().equals(service.service.getClassName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}

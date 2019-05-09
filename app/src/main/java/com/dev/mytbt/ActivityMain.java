package com.dev.mytbt;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.WindowManager;

import com.dev.mytbt.Tools.PermissionManager;
import com.dev.mytbt.ToolsUI.MainPagerAdapter;

public class ActivityMain extends AppCompatActivity {

    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "tbtMainActivity"; // Debug tag

    FragNavigation navFragment;

    /* Lifecycle states
    ----------------------------------------------- */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        PermissionManager.PermissionsGrantedCallback callbackPermissionsGranted = new PermissionManager.PermissionsGrantedCallback() {
            @Override
            public void onPermissionsGranted() {
                /* When the user grants all permissions, or if they were already granted from before, we are ready to continue
                the resume process. */

                // We prevent screen locking while navigating
                getWindow().addFlags( WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        //WindowManager.LayoutParams.FLAG_FULLSCREEN | // uncomment if you want to hide the clock, battery, etc
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD); // we prevent screen locking for navigation

                AppCompatDelegate.setCompatVectorFromResourcesEnabled(true); // enabling svg rendering

                /* If we lost the reference to the navigation fragment, we recreate one, and set up all layout elements as well */
                if (navFragment == null) {
                    navFragment = FragNavigation.newInstance(); // keeping the reference of the navFragment

                    // setting up the viewpager
                    ViewPager viewPager = findViewById(R.id.vpMainViewPager);
                    MainPagerAdapter mainAdapter = new MainPagerAdapter(getSupportFragmentManager());
                    mainAdapter.addFragment(navFragment);
                    viewPager.setAdapter(mainAdapter);
                }
            }

            @Override
            public void onPermissionsNotGranted() {}

            @Override
            public void onAbsoluteRefuse() {
                finish(); // if the user refuses to grant permissions, we simply exit the app
            }
        };

        PermissionManager.requestPermissions(this, callbackPermissionsGranted);
    }

    @Override
    protected void onPause() {
        // resetting window state
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                //WindowManager.LayoutParams.FLAG_FULLSCREEN | // uncomment if you want to hide the clock, battery, etc
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        super.onPause();
    }
}
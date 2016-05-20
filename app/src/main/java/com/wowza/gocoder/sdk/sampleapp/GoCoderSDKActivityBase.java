/**
 *  This code and all components (c) Copyright 2015-2016, Wowza Media Systems, LLC. All rights reserved.
 *  This code is licensed pursuant to the BSD 3-Clause License.
 */
package com.wowza.gocoder.sdk.sampleapp;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import com.wowza.gocoder.sdk.api.WZPlatformInfo;
import com.wowza.gocoder.sdk.api.WowzaGoCoder;
import com.wowza.gocoder.sdk.api.broadcast.WZBroadcast;
import com.wowza.gocoder.sdk.api.broadcast.WZBroadcastConfig;
import com.wowza.gocoder.sdk.api.configuration.WZMediaConfig;
import com.wowza.gocoder.sdk.api.configuration.WowzaConfig;
import com.wowza.gocoder.sdk.api.devices.WZCameraView;
import com.wowza.gocoder.sdk.api.errors.WZStreamingError;
import com.wowza.gocoder.sdk.api.logging.WZLog;
import com.wowza.gocoder.sdk.api.status.WZStatus;
import com.wowza.gocoder.sdk.api.status.WZStatusCallback;
import com.wowza.gocoder.sdk.sampleapp.config.ConfigPrefs;

import java.util.Arrays;

/**
 * The base class for each of the GoCoder SDK demo activities
 */
public abstract class GoCoderSDKActivityBase extends Activity
    implements WZStatusCallback {

    private static String TAG = GoCoderSDKActivityBase.class.getSimpleName();

    private static final String SDK_SAMPLE_APP_LICENSE_KEY = "GSDK-CA41-0001-E32F-0CF1-93EC";
    private static final int PERMISSIONS_REQUEST_CODE = 0x1;

    private static Object sBroadcastLock = new Object();
    private static boolean sBroadcastEnded = true;

    /**
     * The top-level GoCoder SDK API interface
     */
    protected static WowzaGoCoder sGoCoder = null;

    /**
     * An array of Android manifest permission identifiers required for this activity
     */
    protected String[] mRequiredPermissions = {};

    /**
     * Indicates whether this is a full screen activity or note
     */
    protected static boolean sFullScreenActivity = true;

    /**
     * Build an array of WZMediaConfigs from the frame sizes supported by the active camera
     * @param goCoderCameraView the camera view
     * @return an array of WZMediaConfigs from the frame sizes supported by the active camera
     */
    protected static WZMediaConfig[] getVideoConfigs(WZCameraView goCoderCameraView) {
        WZMediaConfig configs[] = WowzaConfig.PRESET_CONFIGS;

        if (goCoderCameraView != null && goCoderCameraView.getCamera() != null) {
            WZMediaConfig cameraConfigs[] = goCoderCameraView.getCamera().getSupportedConfigs();
            Arrays.sort(cameraConfigs);
            configs = cameraConfigs;
        }

        return configs;
    }

    /**
     * The GoCoder SDK broadcaster
     */
    protected WZBroadcast mBroadcast = null;
    public WZBroadcast getBroadcast() {
        return mBroadcast;
    }
    public void setBroadcast(WZBroadcast broadcast) {
        mBroadcast = broadcast;
    }

    /**
     * The GoCoder SDK broadcast configuration
     */
    protected WZBroadcastConfig mBroadcastConfig = null;
    public WZBroadcastConfig getBroadcastConfig() {
        return mBroadcastConfig;
    }
    public void setBroadcastConfig(WZBroadcastConfig broadcastConfig) {
        mBroadcastConfig = broadcastConfig;
    }

    /**
     * Indicates if the required permissions have been granted for application built for Android 6.0 and up
     */
    protected boolean mPermissionsGranted = false;
    public boolean getPermissionsGranted() {
        return mPermissionsGranted;
    }
    public void setPermissionsGranted(boolean permissionsGranted) {
        mPermissionsGranted = permissionsGranted;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the GoCoder SDK
        if (sGoCoder == null) {
            // Enable detailed logging from the GoCoder SDK
            WZLog.LOGGING_ENABLED = true;
            sGoCoder = WowzaGoCoder.init(this, SDK_SAMPLE_APP_LICENSE_KEY);
            if (sGoCoder == null) {
                WZLog.error(TAG, WowzaGoCoder.getLastError());
            } else {
                WZLog.info("GoCoder SDK version number = " + WowzaGoCoder.SDK_VERSION);
                WZLog.info(WowzaGoCoder.PLATFORM_INFO);
                WZLog.info(WowzaGoCoder.OPENGLES_INFO);
                WZLog.info(WZPlatformInfo.displayInfo(this));
            }
        }

        if (sGoCoder != null) {
            // Create a new broadcaster instance
            mBroadcast = new WZBroadcast();

            // Create a new broadcast configuration instance
            mBroadcastConfig = new WZBroadcastConfig(sGoCoder.getConfig());
            mBroadcastConfig.setLogLevel(WZLog.LOG_LEVEL_DEBUG);
        }
    }

    /**
     * Android Activity lifecycle methods
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Check to see if the permissions required for this activity have been granted. If not, invoke the
        // permissions granting workflow necessary for Android 6.0 and up
        if (mBroadcast != null) {
            mPermissionsGranted = (mRequiredPermissions.length > 0 ? WowzaGoCoder.hasPermissions(this, mRequiredPermissions) : true);
            if (mPermissionsGranted) {
                ConfigPrefs.updateConfigFromPrefs(PreferenceManager.getDefaultSharedPreferences(this), mBroadcastConfig);
            } else {
                ActivityCompat.requestPermissions(this,
                        mRequiredPermissions, PERMISSIONS_REQUEST_CODE);
            }
        }
    }


    /**
     * Invoked after the user dismisses the permissions request dialogs
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //
        mPermissionsGranted = true;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                for(int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        mPermissionsGranted = false;
                    }
                }
            }
        }
    }
    @Override
    protected void onPause() {
        // If there is a streaming broadcast is progress, stop it before the application pauses
        if (mBroadcast != null && mBroadcast.getBroadcastStatus().isRunning()) {
            endBroadcast(true);
        }

        super.onPause();
    }

    /**
     * Enable Android's sticky immersive full-screen mode
     * See http://developer.android.com/training/system-ui/immersive.html#sticky
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (sFullScreenActivity && hasFocus) {
            View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
            if (rootView != null)
                rootView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    /**
     * WZStatusCallback interface methods
     */

    /**
     * This method is called each time the broadcast status changes during initialization and shutdown
     */
    @Override
    public void onWZStatus(final WZStatus goCoderStatus) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Log the broadcast status
                WZLog.debug(TAG, goCoderStatus.toString());
            }
        });
    }

    /**
     * This method is called if an error occurs when initializing or running a broadcast
     */
    @Override
    public void onWZError(final WZStatus goCoderStatus) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Log the broadcast error
                WZLog.error(TAG, goCoderStatus.getLastError());
            }
        });
    }

    /**
     * Start a live streaming broadcast session
     */
    protected synchronized WZStreamingError startBroadcast() {
        WZStreamingError configValidationError = null;

        // Make sure the broadcaster is idle first
        if (mBroadcast.getBroadcastStatus().isIdle()) {
            WZLog.debug(TAG, mBroadcastConfig.toString());

            // Validate the broadcast configuration
            configValidationError = mBroadcastConfig.validateForBroadcast();

            if (configValidationError == null) {
                // Tell the broadcaster to start the broadcast
                mBroadcast.startBroadcast(mBroadcastConfig, this);
            }

        } else {
            WZLog.error(TAG, "startBroadcast() called while another broadcast is active");
        }
        return configValidationError;
    }

    /**
     * Shutdown a broadcast session
     *
     * @param appPausing Indicates of the app is currently pausing such as when onPause() is called.
     *                   If set, this method will block until the broadcast is completely shutdown
     */
    protected synchronized void endBroadcast(boolean appPausing) {
        if (!mBroadcast.getBroadcastStatus().isIdle()) {
            if (appPausing) {
                sBroadcastEnded = false;
                // Tell the broadcaster to end the broadcast
                mBroadcast.endBroadcast(new WZStatusCallback() {
                    @Override
                    public void onWZStatus(WZStatus wzStatus) {
                        synchronized (sBroadcastLock) {
                            sBroadcastEnded = true;
                            sBroadcastLock.notifyAll();
                        }
                    }

                    @Override
                    public void onWZError(WZStatus wzStatus) {
                        WZLog.error(TAG, wzStatus.getLastError());
                        synchronized (sBroadcastLock) {
                            sBroadcastEnded = true;
                            sBroadcastLock.notifyAll();
                        }
                    }
                });

                // Block until the broadcast has been completely shutdown
                while(!sBroadcastEnded) {
                    try{
                        sBroadcastLock.wait();
                    } catch (InterruptedException e) {}
                }
            } else {
                mBroadcast.endBroadcast(this);
            }
        }  else {
            WZLog.error(TAG, "endBroadcast() called without an active broadcast");
        }
    }

    /**
     * Shutdown a broadcast session
     */
    protected synchronized void endBroadcast() {
        endBroadcast(false);
    }
}

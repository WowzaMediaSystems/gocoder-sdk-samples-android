/**
 *  This is sample code provided by Wowza Media Systems, LLC.  All sample code is intended to be a reference for the
 *  purpose of educating developers, and is not intended to be used in any production environment.
 *
 *  IN NO EVENT SHALL WOWZA MEDIA SYSTEMS, LLC BE LIABLE TO YOU OR ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
 *  OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION,
 *  EVEN IF WOWZA MEDIA SYSTEMS, LLC HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  WOWZA MEDIA SYSTEMS, LLC SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. ALL CODE PROVIDED HEREUNDER IS PROVIDED "AS IS".
 *  WOWZA MEDIA SYSTEMS, LLC HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 *  Copyright © 2015 Wowza Media Systems, LLC. All rights reserved.
 */

package com.wowza.gocoder.sdk.sampleapp;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.WindowManager;

import com.wowza.gocoder.sdk.api.WZPlatformInfo;
import com.wowza.gocoder.sdk.api.WowzaGoCoder;
import com.wowza.gocoder.sdk.api.broadcast.WZBroadcast;
import com.wowza.gocoder.sdk.api.broadcast.WZBroadcastConfig;
import com.wowza.gocoder.sdk.api.configuration.WZMediaConfig;
import com.wowza.gocoder.sdk.api.configuration.WowzaConfig;
import com.wowza.gocoder.sdk.api.data.WZDataItem;
import com.wowza.gocoder.sdk.api.data.WZDataMap;
import com.wowza.gocoder.sdk.api.devices.WZCameraView;
import com.wowza.gocoder.sdk.api.errors.WZStreamingError;
import com.wowza.gocoder.sdk.api.logging.WZLog;
import com.wowza.gocoder.sdk.api.status.WZStatus;
import com.wowza.gocoder.sdk.api.status.WZStatusCallback;
import com.wowza.gocoder.sdk.sampleapp.config.ConfigPrefs;

import java.util.Arrays;
import java.util.Date;

public abstract class GoCoderSDKActivityBase extends Activity
    implements WZStatusCallback {

    private final static String TAG = GoCoderSDKActivityBase.class.getSimpleName();

    private static final String SDK_SAMPLE_APP_LICENSE_KEY = "GOSK-5442-0101-750D-4A14-FB5C";
    private static final int PERMISSIONS_REQUEST_CODE = 0x1;

    protected String[] mRequiredPermissions = {};

    private static Object sBroadcastLock = new Object();
    private static boolean sBroadcastEnded = true;

    // indicates whether this is a full screen activity or not
    protected static boolean sFullScreenActivity = true;

    // GoCoder SDK top level interface
    protected static WowzaGoCoder sGoCoderSDK = null;

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

    protected boolean mPermissionsGranted = false;

    protected WZBroadcast mWZBroadcast = null;
    public WZBroadcast getBroadcast() {
        return mWZBroadcast;
    }

    protected WZBroadcastConfig mWZBroadcastConfig = null;
    public WZBroadcastConfig getBroadcastConfig() {
        return mWZBroadcastConfig;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (sGoCoderSDK == null) {
            // Enable detailed logging from the GoCoder SDK
            WZLog.LOGGING_ENABLED = true;

            // Initialize the GoCoder SDK
            sGoCoderSDK = WowzaGoCoder.init(this, SDK_SAMPLE_APP_LICENSE_KEY);

            if (sGoCoderSDK == null) {
                WZLog.error(TAG, WowzaGoCoder.getLastError());
            }
        }

        if (sGoCoderSDK != null) {
            // Create a GoCoder broadcaster and an associated broadcast configuration
            mWZBroadcast = new WZBroadcast();
            mWZBroadcastConfig = new WZBroadcastConfig(sGoCoderSDK.getConfig());
            mWZBroadcastConfig.setLogLevel(WZLog.LOG_LEVEL_DEBUG);
        }
    }

    /**
     * Android Activity lifecycle methods
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (mWZBroadcast != null) {
            mPermissionsGranted = true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mPermissionsGranted = (mRequiredPermissions.length > 0 ? WowzaGoCoder.hasPermissions(this, mRequiredPermissions) : true);
                if (!mPermissionsGranted)
                    ActivityCompat.requestPermissions(this, mRequiredPermissions, PERMISSIONS_REQUEST_CODE);
            }

            if (mPermissionsGranted) {
                ConfigPrefs.updateConfigFromPrefs(PreferenceManager.getDefaultSharedPreferences(this), mWZBroadcastConfig);
            }
        }
    }

    @Override
    protected void onPause() {
        // Stop any active live stream
        if (mWZBroadcast != null && mWZBroadcast.getStatus().isRunning()) {
            endBroadcast(true);
        }

        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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
    @Override
    public void onWZStatus(final WZStatus goCoderStatus) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (goCoderStatus.isReady()) {
                    // Keep the screen on while the broadcast is active
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    // Since we have successfully opened up the server connection, store the connection info for auto complete
                    ConfigPrefs.storeAutoCompleteHostConfig(PreferenceManager.getDefaultSharedPreferences(GoCoderSDKActivityBase.this), mWZBroadcastConfig);
                } else if (goCoderStatus.isIdle())
                    // Clear the "keep screen on" flag
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                WZLog.debug(TAG, goCoderStatus.toString());
            }
        });
    }

    @Override
    public void onWZError(final WZStatus goCoderStatus) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                WZLog.error(TAG, goCoderStatus.getLastError());
            }
        });
    }

    protected synchronized WZStreamingError startBroadcast() {
        WZStreamingError configValidationError = null;

        if (mWZBroadcast.getStatus().isIdle()) {
            WZLog.info(TAG, "=============== Broadcast Configuration ===============\n"
                    + mWZBroadcastConfig.toString()
                        + "\n=======================================================");

            configValidationError = mWZBroadcastConfig.validateForBroadcast();
            if (configValidationError == null)
                mWZBroadcast.startBroadcast(mWZBroadcastConfig, this);
        } else {
            WZLog.error(TAG, "startBroadcast() called while another broadcast is active");
        }
        return configValidationError;
    }

    protected synchronized void endBroadcast(boolean appPausing) {
        if (!mWZBroadcast.getStatus().isIdle()) {
            if (appPausing) {
                // Stop any active live stream
                sBroadcastEnded = false;
                mWZBroadcast.endBroadcast(new WZStatusCallback() {
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

                while(!sBroadcastEnded) {
                    try{
                        sBroadcastLock.wait();
                    } catch (InterruptedException e) {}
                }
            } else {
                mWZBroadcast.endBroadcast(this);
            }
        }  else {
            WZLog.error(TAG, "endBroadcast() called without an active broadcast");
        }
    }

    protected synchronized void endBroadcast() {
        endBroadcast(false);
    }
}

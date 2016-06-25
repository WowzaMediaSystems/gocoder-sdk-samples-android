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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.WowzaGoCoder;
import com.wowza.gocoder.sdk.api.configuration.WZMediaConfig;
import com.wowza.gocoder.sdk.api.devices.WZAudioDevice;
import com.wowza.gocoder.sdk.api.devices.WZCamera;
import com.wowza.gocoder.sdk.api.devices.WZCameraView;
import com.wowza.gocoder.sdk.api.devices.WZDeviceUtils;
import com.wowza.gocoder.sdk.api.encoder.WZEncoderAPI;
import com.wowza.gocoder.sdk.api.errors.WZStreamingError;
import com.wowza.gocoder.sdk.api.h264.WZProfileLevel;
import com.wowza.gocoder.sdk.api.logging.WZLog;
import com.wowza.gocoder.sdk.api.status.WZStatus;
import com.wowza.gocoder.sdk.sampleapp.config.ConfigPrefs;
import com.wowza.gocoder.sdk.sampleapp.config.ConfigPrefsActivity;
import com.wowza.gocoder.sdk.sampleapp.ui.MultiStateButton;
import com.wowza.gocoder.sdk.sampleapp.ui.StatusView;

import java.util.ArrayList;
import java.util.Arrays;

abstract public class CameraActivityBase extends GoCoderSDKActivityBase {
    private final static String TAG = CameraActivityBase.class.getSimpleName();

    // UI controls
    protected MultiStateButton      mBtnBroadcast     = null;
    protected MultiStateButton      mBtnSettings      = null;
    protected StatusView            mStatusView       = null;

    // The GoCoder SDK camera preview display view
    protected WZCameraView    mWZCameraView     = null;
    protected WZAudioDevice   mWZAudioDevice    = null;

    private boolean mDevicesInitialized = false;
    private boolean mUIInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Android Activity lifecycle methods
     */
    @Override
    protected void onResume() {
        super.onResume();

        initUIControls();
        initGoCoderDevices();

        initGoCoderCameraPreview();

        syncUIControlState();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mWZCameraView != null) {
            mWZCameraView.onPause();
        }
    }

    /**
     * WZStatusCallback interface methods
     */
    @Override
    public void onWZStatus(final WZStatus goCoderStatus) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (goCoderStatus.isRunning()) {
                    // Keep the screen on while we are broadcasting
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else if (goCoderStatus.isIdle()) {
                    // Clear the "keep screen on" flag
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }

                if (mStatusView != null) mStatusView.setStatus(goCoderStatus);
                syncUIControlState();
            }
        });
    }

    @Override
    public void onWZError(final WZStatus goCoderStatus) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mStatusView != null) mStatusView.setStatus(goCoderStatus);
                syncUIControlState();
            }
        });
    }

    /**
     * Click handler for the broadcast button
     */
    public void onToggleBroadcast(View v) {
        if (getBroadcast() == null) return;

        if (getBroadcast().getStatus().isIdle()) {
            // Update the video source orientation in the broadcast config with the current device orientation
            getBroadcastConfig().setVideoSourceOrientation(WZDeviceUtils.getDeviceOrientation(this));

            WZStreamingError configError = startBroadcast();
            if (configError != null) {
                if (mStatusView != null) mStatusView.setErrorMessage(configError.getErrorDescription());
            }
        } else {
            endBroadcast();
        }
    }

    /**
     * Click handler for the settings button
     */
    public void onSettings(View v) {
        if (sGoCoderSDK == null) return;

        WZMediaConfig configs[] = (mWZCameraView != null ? getVideoConfigs(mWZCameraView) : new WZMediaConfig[0]);

        int avcProfiles[] = WZEncoderAPI.getProfiles();
        if (avcProfiles.length > 1) Arrays.sort(avcProfiles);

        WZProfileLevel avcProfileLevels[] = WZEncoderAPI.getProfileLevels();
        if (avcProfileLevels.length > 1) Arrays.sort(avcProfileLevels);

        ArrayList<WZProfileLevel> allProfileLevels = new ArrayList<>();

        if (avcProfiles.length > 0) {
            for (int avcProfile : avcProfiles) {
                WZProfileLevel autoLevel = new WZProfileLevel(avcProfile, WZProfileLevel.PROFILE_LEVEL_AUTO);
                allProfileLevels.add(autoLevel);

                for (WZProfileLevel avcProfileLevel : avcProfileLevels) {
                    if (avcProfileLevel.getProfile() == avcProfile) {
                        allProfileLevels.add(avcProfileLevel);
                    }
                }
            }
        }

        avcProfileLevels = allProfileLevels.toArray(new WZProfileLevel[allProfileLevels.size()]);

        Intent intent = new Intent(this, ConfigPrefsActivity.class);
        intent.putExtra(ConfigPrefs.PREFS_TYPE, ConfigPrefs.ALL_PREFS);
        intent.putExtra(ConfigPrefs.VIDEO_CONFIGS, configs);
        intent.putExtra(ConfigPrefs.H264_PROFILE_LEVELS,  avcProfileLevels);
        startActivity(intent);
    }

    protected void initGoCoderDevices() {
        if (sGoCoderSDK != null && mPermissionsGranted && !mDevicesInitialized) {

            // Create an audio input device instance
            mWZAudioDevice = new WZAudioDevice();
            // Init the broadcast config audio broadcaster
            mWZBroadcastConfig.setAudioBroadcaster(mWZAudioDevice);

            // Log information about the cameras discovered on the local device
            WZLog.info(TAG, "=================== Camera Information ===================\n"
                    + WZCamera.getCameraInfo()
                    + "\n==========================================================");

            // Let's check if we were able to access all of the cameras on this device
            ArrayList<String> badCameras = new ArrayList<>();
            WZCamera[] allCameras = WZCamera.getDeviceCameras();
            for (int c = 0; c < allCameras.length; c++) {
                if (!allCameras[c].isAvailable()) {
                    badCameras.add(allCameras[c].getDirection() == WZCamera.DIRECTION_FRONT ? "front" : "back");
                }
            }

            // Let's check to see if the any of the cameras could not be opened correctly
            if (badCameras.size() > 0) {
                TextUtils.join(" and ", badCameras);
                mStatusView.setErrorMessage("There was an error attempting to initialize the " +
                        TextUtils.join(" and ", badCameras) +
                        " camera" + (badCameras.size() > 1 ? "s" : ""));
            }

            mDevicesInitialized = true;
        }
    }

    protected void initGoCoderCameraPreview() {
        if (sGoCoderSDK != null && mPermissionsGranted && mWZCameraView != null) {
            sGoCoderSDK.setCameraView(mWZCameraView);

            // Initialize the camera view
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

            // Ensure we can access to at least one camera
            int numberOfCameras = mWZCameraView.getCameras().length;
            if (numberOfCameras > 1) {
                int activeCameraId = ConfigPrefs.getActiveCamera(sharedPrefs);
                WZCamera activeCamera = mWZCameraView.getCameraById(activeCameraId);
                if (activeCamera != null && activeCamera.isAvailable())
                    mWZCameraView.setCamera(activeCameraId);
            } else if (numberOfCameras < 1) {
                mStatusView.setErrorMessage("Could not detect or gain access to any cameras on this device");
                getBroadcastConfig().setVideoEnabled(false);
            } else if (numberOfCameras < 1)
                getBroadcastConfig().setVideoEnabled(false);

            mWZCameraView.setCameraConfig(mWZBroadcastConfig);
            mWZCameraView.setScaleMode(ConfigPrefs.getScaleMode(sharedPrefs));

            // If video is enabled in the current broadcast configuration, turn on the camera preview
            if (getBroadcastConfig().isVideoEnabled()) {

                if (mWZCameraView.isPaused())
                    mWZCameraView.onResume();
                else
                    mWZCameraView.startPreview();

                // Briefly display the video frame size from config
                Toast.makeText(this, mWZBroadcastConfig.getLabel(true, true, false, true), Toast.LENGTH_LONG).show();

                // Turn on continuous auto focus if the camera supports it
                WZCamera activeCamera = mWZCameraView.getCamera();
                if (activeCamera != null && activeCamera.hasCapability(WZCamera.FOCUS_MODE_CONTINUOUS)) {
                    activeCamera.setFocusMode(WZCamera.FOCUS_MODE_CONTINUOUS);
                }
            }

            // Init the broadcast config video broadcaster
            if (getBroadcastConfig().isVideoEnabled()) {
                mWZBroadcastConfig.setVideoBroadcaster(mWZCameraView.getBroadcaster());
            }
        }
    }

    protected void initUIControls() {
        if (mUIInitialized) return;

        // Initialize the UI controls
        mBtnBroadcast       = (MultiStateButton) findViewById(R.id.ic_broadcast);
        mBtnSettings        = (MultiStateButton) findViewById(R.id.ic_settings);
        mStatusView         = (StatusView) findViewById(R.id.statusView);

        mWZCameraView       = (WZCameraView) findViewById(R.id.cameraPreview);

        mUIInitialized      = true;

        if (sGoCoderSDK == null && mStatusView != null)
            mStatusView.setErrorMessage(WowzaGoCoder.getLastError().getErrorDescription());
    }

    protected boolean syncUIControlState() {
        boolean disableControls = (getBroadcast() == null ||
                !(getBroadcast().getStatus().isIdle() ||
                        getBroadcast().getStatus().isRunning()));
        boolean isStreaming = getBroadcast().getStatus().isRunning();

        if (disableControls) {
            if (mBtnBroadcast != null) mBtnBroadcast.setEnabled(false);
            if (mBtnSettings != null) mBtnSettings.setEnabled(false);
        } else {
            if (mBtnBroadcast != null) {
                mBtnBroadcast.setState(isStreaming);
                mBtnBroadcast.setEnabled(true);
            }
            if (mBtnSettings != null)
                mBtnSettings.setEnabled(!isStreaming);
        }

        return disableControls;
    }
}

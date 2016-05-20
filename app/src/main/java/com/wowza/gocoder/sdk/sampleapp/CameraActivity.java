/**
 *  This code and all components (c) Copyright 2015-2016, Wowza Media Systems, LLC. All rights reserved.
 *  This code is licensed pursuant to the BSD 3-Clause License.
 */
package com.wowza.gocoder.sdk.sampleapp;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.view.GestureDetectorCompat;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.devices.WZCameraView;
import com.wowza.gocoder.sdk.api.configuration.WZMediaConfig;
import com.wowza.gocoder.sdk.api.configuration.WowzaConfig;
import com.wowza.gocoder.sdk.api.WowzaGoCoder;
import com.wowza.gocoder.sdk.api.devices.WZCamera;
import com.wowza.gocoder.sdk.api.encoder.WZEncoderAPI;
import com.wowza.gocoder.sdk.api.h264.WZProfileLevel;
import com.wowza.gocoder.sdk.api.logging.WZLog;
import com.wowza.gocoder.sdk.api.status.WZStatus;
import com.wowza.gocoder.sdk.api.errors.WZStreamingError;
import com.wowza.gocoder.sdk.sampleapp.config.ConfigPrefs;
import com.wowza.gocoder.sdk.sampleapp.config.ConfigPrefsActivity;
import com.wowza.gocoder.sdk.sampleapp.ui.AutoFocusListener;
import com.wowza.gocoder.sdk.sampleapp.ui.ControlButton;
import com.wowza.gocoder.sdk.sampleapp.ui.StatusView;
import com.wowza.gocoder.sdk.sampleapp.ui.TimerView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This activity demonstrates the primary camera, audio, and live streaming capabilities of the GoCoder SDK
 */
public class CameraActivity extends GoCoderSDKActivityBase {
    private static String TAG = CameraActivity.class.getSimpleName();

    protected int SCALE_MODES[] = {
            WowzaConfig.FILL_FRAME,
            WowzaConfig.CROP_TO_FRAME
    };
    protected int mScaleModeIndex = 0;

    // UI controls
    protected ControlButton   mBtnBroadcast         = null;
    protected ControlButton   mBtnSettings          = null;
    protected ControlButton   mBtnSwitchCamera      = null;
    protected ControlButton   mBtnTorch             = null;
    protected ControlButton   mBtnMic               = null;
    protected StatusView      mStatusView           = null;
    protected TimerView       mTimerView            = null;
    protected Button          mBtnScale             = null;

    // The GoCoder SDK camera preview display view
    protected WZCameraView mGoCoderCameraView = null;

    // Gestures are used to toggle the focus modes
    protected GestureDetectorCompat mAutoFocusDetector = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_camera);
        super.onCreate(savedInstanceState);

        // Set the permission required by this activity
        mRequiredPermissions = new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };

        // Initialize the UI controls
        mBtnBroadcast       = new ControlButton(this, R.id.ic_broadcast, false, false, R.drawable.ic_stop, R.drawable.ic_start);
        mBtnSettings        = new ControlButton(this, R.id.ic_settings, true);
        mBtnSwitchCamera    = new ControlButton(this, R.id.ic_switch_camera, false);
        mBtnTorch           = new ControlButton(this, R.id.ic_torch, false, false, R.drawable.ic_torch_on, R.drawable.ic_torch_off);
        mBtnMic             = new ControlButton(this, R.id.ic_mic, false, true, R.drawable.ic_mic_on, R.drawable.ic_mic_off);

        mStatusView         = (StatusView) findViewById(R.id.statusView);
        mTimerView          = (TimerView) findViewById(R.id.txtTimer);

        mBtnScale           = (Button) findViewById(R.id.btnScale);

        if (sGoCoder != null) {
            // Get the WZCameraView instance defined in the view layout
            mGoCoderCameraView = (WZCameraView) findViewById(R.id.cameraPreview);
            // Set the camera view property in the SDK
            sGoCoder.setCameraView(mGoCoderCameraView);

            // Configure the broadcaster with the default configuration
            getBroadcastConfig().set(sGoCoder.getDefaultBroadcastConfig());
            // Configure the broadcaster to use the camera view as the video source
            getBroadcastConfig().setVideoBroadcaster(mGoCoderCameraView.getBroadcaster());

            // Create a gesture detector to use for touch events and focusing the camera
            mAutoFocusDetector = new GestureDetectorCompat(this, new AutoFocusListener(this, mGoCoderCameraView));
        } else {
            mStatusView.setErrorMessage(WowzaGoCoder.getLastError().getErrorDescription());
        }
    }

    /**
     * Android Activity lifecycle methods
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (mGoCoderCameraView != null && mPermissionsGranted) {
            // Log information about the cameras discovered on the local device
            WZLog.info(TAG, WZCameraView.getCameraInfo());

            // Let's check if we were able to access all of the cameras on this device
            ArrayList<String> badCameras = new ArrayList<>();
            WZCamera[] allCameras = WZCameraView.getDeviceCameras();
            for(int c=0; c<allCameras.length; c++) {
                if (!allCameras[c].isAvailable()) {
                    badCameras.add(allCameras[c].getDirection() == WZCamera.DIRECTION_FRONT ? "front" : "back");
                }
            }
            if (badCameras.size() > 0) {
                TextUtils.join(" and ", badCameras);
                mStatusView.setErrorMessage("There was an error attempting to initialize the " +
                        TextUtils.join(" and ", badCameras) +
                        " camera" + (badCameras.size()>1?"s":""));
            }

            // Initialize the camera view
            int numberOfCameras = mGoCoderCameraView.getCameras().length;
            if (numberOfCameras > 1) {
                // Get the last camera used from the prefs or use the default one
                int activeCameraId = ConfigPrefs.getActiveCamera(PreferenceManager.getDefaultSharedPreferences(this));
                WZCamera activeCamera = mGoCoderCameraView.getCameraById(activeCameraId);
                if (activeCamera != null && activeCamera.isAvailable())
                    mGoCoderCameraView.setCamera(activeCameraId);
            } else if (numberOfCameras < 1) {
                mStatusView.setErrorMessage("Could not detect or gain access to any cameras on this device");
                getBroadcastConfig().setVideoEnabled(false);
            } else if (numberOfCameras < 1)
                getBroadcastConfig().setVideoEnabled(false);

            // Set the camera frame size and rate based on the broadcast configuration
            mGoCoderCameraView.setCameraConfig(getBroadcastConfig());
            mGoCoderCameraView.setScaleMode(SCALE_MODES[mScaleModeIndex]);

            // If video is enabled in the current broadcast configuration, turn on the camera preview
            if (getBroadcastConfig().isVideoEnabled()) {
                mGoCoderCameraView.startPreview();

                // Briefly display the video frame size from config
                Toast.makeText(this, getBroadcastConfig().getLabel(true, true, false, true), Toast.LENGTH_LONG).show();

                // Turn on continuous video focus if the camera supports it
                WZCamera activeCamera = mGoCoderCameraView.getCamera();
                if (activeCamera != null && activeCamera.hasCapability(WZCamera.FOCUS_MODE_CONTINUOUS)) {
                    activeCamera.setFocusMode(WZCamera.FOCUS_MODE_CONTINUOUS);
                }
            }

            updateUIControls();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop the camera preview and release the camera before the app pauses
        if (mGoCoderCameraView != null)
            mGoCoderCameraView.stopPreview();
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
                if (goCoderStatus.isRunning()) {
                    // Keep the screen on while we are broadcasting
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else if (goCoderStatus.isIdle()) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                mStatusView.setStatus(goCoderStatus);
                updateUIControls();
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
                mStatusView.setStatus(goCoderStatus);
                updateUIControls();
            }
        });
    }

    /**
     * Click handler for the broadcast button
     */
    public void onToggleBroadcast(View v) {
        if (getBroadcast() == null) return;

        // If the broadcast is idle, start a new live streaming broadcast
        if (getBroadcast().getBroadcastStatus().isIdle()) {
            // Update the video source orientation in the broadcast config with the current device orientation
            getBroadcastConfig().setVideoSourceOrientation(mGoCoderCameraView.getDeviceOrientation());

            WZStreamingError configError = startBroadcast();
            // Check to ensure a broadcast configuration validation error did not occur
            if (configError != null) {
                mStatusView.setErrorMessage(configError.getErrorDescription());
            }
        } else {
            // stop the active broadcast
            endBroadcast();
        }
    }

    /**
     * Click handler for the settings button
     */
    public void onSettings(View v) {
        if (getBroadcastConfig() == null) return;

        WZMediaConfig configs[] = getVideoConfigs(mGoCoderCameraView);

        // Gather the supported H.264 profiles and profile levels for display in the video config prefs
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

    /**
     * Click handler for the switch camera button
     */
    public void onSwitchCamera(View v) {
        if (mGoCoderCameraView == null) return;

        WZCamera newCamera = mGoCoderCameraView.switchCamera();
        if (newCamera != null)
            ConfigPrefs.setActiveCamera(PreferenceManager.getDefaultSharedPreferences(this), newCamera.getCameraId());

        boolean hasTorch = (newCamera != null && newCamera.hasCapability(WZCamera.TORCH));
        mBtnTorch.setEnabled(hasTorch);
        if (hasTorch) {
            mBtnTorch.setStateOn(newCamera.isTorchOn());
        }
        mBtnTorch.setVisible(hasTorch);
    }

    /**
     * Click handler for the torch/flashlight button
     */
    public void onToggleTorch(View v) {
        if (mGoCoderCameraView == null) return;

        WZCamera activeCamera = mGoCoderCameraView.getCamera();
        activeCamera.setTorchOn(mBtnTorch.toggleState());
    }

    /**
     * Click handler for the mic/mute button
     */
    public void onToggleMute(View v) {
        if (sGoCoder == null) return;
        sGoCoder.getDefaultAudioDevice().setMuted(!mBtnMic.toggleState());
    }

    /**
     * Click handler for the scale mode button
     */
    public void onScaleMode(View v) {
        if (mGoCoderCameraView == null) return;

        mScaleModeIndex = ++mScaleModeIndex % SCALE_MODES.length;
        mGoCoderCameraView.setScaleMode(SCALE_MODES[mScaleModeIndex]);

        Button btn = (Button) findViewById(R.id.btnScale);
        switch(SCALE_MODES[mScaleModeIndex]) {
            case WowzaConfig.FILL_FRAME:
                btn.setText("Fill mode");
                break;
            case WowzaConfig.CROP_TO_FRAME:
                btn.setText("Crop mode");
                break;
        }
    }

    /**
     * Send any touch events to the auto focus listener
     */
    @Override
    public boolean onTouchEvent(MotionEvent event){
        mAutoFocusDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    /**
     * Update the state of the UI controls
     */
    protected boolean updateUIControls() {
        boolean disableControls = (getBroadcast() == null ||
                !(getBroadcast().getBroadcastStatus().isIdle() ||
                        getBroadcast().getBroadcastStatus().isRunning()));

        if (disableControls) {
            // Disable the controls during broadcast stage transitions
            mBtnBroadcast.setEnabled(false);
            mBtnSwitchCamera.setEnabled(false);
            mBtnTorch.setEnabled(false);
            mBtnMic.setVisible(false);
            mBtnSettings.setEnabled(false);
            mBtnScale.setEnabled(false);
        } else {
            boolean isStreaming = getBroadcast().getBroadcastStatus().isRunning();

            mBtnBroadcast.setStateOn(isStreaming);
            mBtnBroadcast.setEnabled(true);
            mBtnSettings.setEnabled(!isStreaming);

            boolean isDisplayingVideo = (getBroadcastConfig().isVideoEnabled() && mGoCoderCameraView.getCameras().length > 0);
            if (isDisplayingVideo) {
                WZCamera activeCamera = mGoCoderCameraView.getCamera();

                // Set the torch icon based on device support and the torch's current state
                boolean hasTorch = (activeCamera != null && activeCamera.hasCapability(WZCamera.TORCH));
                mBtnTorch.setEnabled(hasTorch);
                if (hasTorch) {
                    mBtnTorch.setStateOn(activeCamera.isTorchOn());
                }
                mBtnTorch.setVisible(hasTorch);

                // Set the switch camera based on device availability
                mBtnSwitchCamera.setEnabled(mGoCoderCameraView.isSwitchCameraAvailable());
                mBtnSwitchCamera.setVisible(mGoCoderCameraView.isSwitchCameraAvailable());
            } else {
                mBtnSwitchCamera.setVisible(false);
                mBtnTorch.setVisible(false);
            }

            mBtnScale.setVisibility(isDisplayingVideo ? View.VISIBLE : View.INVISIBLE);
            mBtnScale.setEnabled(isDisplayingVideo);

            // Set the mic icon based on audio being enabled and whether a stream is active or not
            boolean isStreamingAudio = (isStreaming && getBroadcastConfig().isAudioEnabled());
            mBtnMic.setEnabled(isStreamingAudio);
            mBtnMic.setStateOn(!sGoCoder.getDefaultAudioDevice().isMuted());
            mBtnMic.setVisible(isStreamingAudio);

            // Start or stop the timer based on the broadcaster state
            if (isStreaming && !mTimerView.isRunning()) {
                mTimerView.startTimer();
            } else if (getBroadcast().getBroadcastStatus().isIdle() && mTimerView.isRunning()) {
                mTimerView.stopTimer();
            }
        }

        return disableControls;
    }

}

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
 *  © 2015 – 2019 Wowza Media Systems, LLC. All rights reserved.
 */

package com.wowza.gocoder.sdk.sampleapp;

import android.Manifest;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.WowzaGoCoder;
import com.wowza.gocoder.sdk.api.configuration.WOWZMediaConfig;
import com.wowza.gocoder.sdk.api.devices.WOWZAudioDevice;
import com.wowza.gocoder.sdk.api.devices.WOWZCamera;
import com.wowza.gocoder.sdk.api.devices.WOWZCameraView;
import com.wowza.gocoder.sdk.api.errors.WOWZError;
import com.wowza.gocoder.sdk.api.errors.WOWZStreamingError;
import com.wowza.gocoder.sdk.api.geometry.WOWZSize;
import com.wowza.gocoder.sdk.api.graphics.WOWZColor;
import com.wowza.gocoder.sdk.api.logging.WOWZLog;
import com.wowza.gocoder.sdk.api.status.WOWZBroadcastStatus;
import com.wowza.gocoder.sdk.api.status.WOWZBroadcastStatusCallback;
import com.wowza.gocoder.sdk.support.status.WOWZStatus;
import com.wowza.gocoder.sdk.sampleapp.config.GoCoderSDKPrefs;
import com.wowza.gocoder.sdk.sampleapp.ui.MultiStateButton;
import com.wowza.gocoder.sdk.sampleapp.ui.StatusView;

import java.util.Arrays;

abstract public class CameraActivityBase extends GoCoderSDKActivityBase
        implements WOWZCameraView.PreviewStatusListener, WOWZBroadcastStatusCallback {


    private final static String[] CAMERA_CONFIG_PREFS_SORTED = new String[]{"wz_video_enabled", "wz_video_frame_size", "wz_video_preset"};

    // UI controls
    protected MultiStateButton mBtnBroadcast = null;
    protected MultiStateButton mBtnSettings  = null;
    protected StatusView       mStatusView   = null;

    // The GoCoder SDK camera preview display view
    protected WOWZCameraView mWZCameraView  = null;
    protected WOWZAudioDevice mWZAudioDevice = null;

    private boolean mDevicesInitialized = false;
    private boolean mUIInitialized      = false;

    private SharedPreferences.OnSharedPreferenceChangeListener mPrefsChangeListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefsFragment = new GoCoderSDKPrefs.PrefsFragment();
    }


    @Override
    public void onWZStatus(WOWZBroadcastStatus status) {
        WOWZLog.debug("BroadcastStateMachine[CameraActivityBase] : onWZStatus : "+status.toString());
        syncUIControlState();
    }

    @Override
    public void onWZError(WOWZBroadcastStatus status) {
        WOWZLog.debug("BroadcastStateMachine[CameraActivityBase] : onWZError : "+status.toString());
        syncUIControlState();
    }
    //define callback interface
    interface PermissionCallbackInterface {

        void onPermissionResult(boolean result);
    }

    /**
     * Android Activity lifecycle methods
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (!mUIInitialized) {
            initUIControls();
        }
        if (!mDevicesInitialized) {
            initGoCoderDevices();
        }

        this.hasDevicePermissionToAccess(new PermissionCallbackInterface() {

            @Override
            public void onPermissionResult(boolean result) {
                if (!mDevicesInitialized || result) {
                    initGoCoderDevices();
                }
            }
        });

        if (sGoCoderSDK != null && this.hasDevicePermissionToAccess(Manifest.permission.CAMERA)) {
            final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

            /// Set mirror capability.
            //mWZCameraView.setSurfaceExtension(mWZCameraView.EXTENSION_MIRROR);

            // Update the camera preview display config based on the stored shared preferences

            mWZCameraView.setCameraConfig(getBroadcastConfig());
            mWZCameraView.setScaleMode(GoCoderSDKPrefs.getScaleMode(sharedPrefs));
            mWZCameraView.setVideoBackgroundColor(WOWZColor.DARKGREY);

            CameraActivityBase base = this;


            // Setup up a shared preferences change listener to update the camera preview
            // as the related preference values change
            mPrefsChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String prefsKey) {
                    try {
                        if (prefsKey.equalsIgnoreCase("wz_live_host_address")) {
                            String host = sharedPreferences.getString("wz_live_host_address", String.valueOf(getBroadcastConfig().getHostAddress()));
                            getBroadcastConfig().setHostAddress(host);
                            mWZBroadcastConfig.setHostAddress(host);
                        }
                        if (prefsKey.equalsIgnoreCase("wz_live_port_number")) {

                            String port = sharedPreferences.getString("wz_live_port_number", String.valueOf(getBroadcastConfig().getPortNumber()));
                            getBroadcastConfig().setPortNumber(Integer.parseInt(port));
                            mWZBroadcastConfig.setPortNumber(Integer.parseInt(port));
                        }
                        if (prefsKey.equalsIgnoreCase("wz_live_app_name")) {

                            String appName = sharedPreferences.getString("wz_live_app_name", String.valueOf(getBroadcastConfig().getApplicationName()));
                            getBroadcastConfig().setApplicationName(appName);
                            mWZBroadcastConfig.setApplicationName(appName);
                        }
                        if (prefsKey.equalsIgnoreCase("wz_live_stream_name")) {
                            String streamName = sharedPreferences.getString("wz_live_stream_name", String.valueOf(getBroadcastConfig().getApplicationName()));
                            getBroadcastConfig().setStreamName(streamName);
                            mWZBroadcastConfig.setStreamName(streamName);
                        }
                    }
                    catch(Exception ex)
                    {
                        WOWZLog.error(ex);
                    }
                    if (mWZCameraView != null &&  Arrays.binarySearch(CAMERA_CONFIG_PREFS_SORTED, prefsKey) != -1) {
                        if(prefsKey.equalsIgnoreCase("wz_video_framerate")){
                            String currentFrameRate = String.valueOf(mWZCameraView.getFramerate());
                            String frameRate = sharedPreferences.getString("wz_video_framerate",currentFrameRate);
                            getBroadcastConfig().setVideoFramerate(Integer.parseInt(frameRate));
                            mWZBroadcastConfig.setVideoFramerate(Integer.parseInt(frameRate));
                            mWZBroadcastConfig.getVideoSourceConfig().setVideoFramerate(Integer.parseInt(frameRate));
                        }

                        if(prefsKey.equalsIgnoreCase("wz_video_bitrate")){
                            String currentBitrate = String.valueOf(mWZCameraView.getFramerate());
                            String bitrate = sharedPreferences.getString("wz_video_bitrate",currentBitrate);
                            getBroadcastConfig().setVideoBitRate(Integer.parseInt(bitrate));
                            mWZBroadcastConfig.setVideoFramerate(Integer.parseInt(bitrate));
                            mWZBroadcastConfig.getVideoSourceConfig().setVideoFramerate(Integer.parseInt(bitrate));
                        }

                        // Update the camera preview display frame size

                        if(prefsKey.equalsIgnoreCase("wz_video_frame_width") || prefsKey.equalsIgnoreCase("wz_video_frame_height")) {
                            WOWZSize currentFrameSize = mWZCameraView.getFrameSize();
                            int prefsFrameWidth = sharedPreferences.getInt("wz_video_frame_width", currentFrameSize.getWidth());
                            int prefsFrameHeight = sharedPreferences.getInt("wz_video_frame_height", currentFrameSize.getHeight());
                            WOWZSize prefsFrameSize = new WOWZSize(prefsFrameWidth, prefsFrameHeight);
                            if (!prefsFrameSize.equals(currentFrameSize))
                                mWZCameraView.setFrameSize(prefsFrameSize);
                        }
                        if(prefsKey.equalsIgnoreCase("wz_video_resize_to_aspect") ) {
                            Boolean scaleMode = sharedPreferences.getBoolean("wz_video_resize_to_aspect", false); // ? WOWZMediaConfig.RESIZE_TO_ASPECT : WOWZMediaConfig.FILL_VIEW;
                            if (scaleMode) {
                                mWZCameraView.setScaleMode(WOWZMediaConfig.RESIZE_TO_ASPECT);  //WOWZMediaConfig.RESIZE_TO_ASPECT :
                            } else {
                                mWZCameraView.setScaleMode(WOWZMediaConfig.FILL_VIEW);  //WOWZMediaConfig.RESIZE_TO_ASPECT :
                            }
                        }

                        mWZCameraView.setCameraConfig(mWZBroadcastConfig);

                        // Toggle the camera preview on or off
                        boolean videoEnabled = sharedPreferences.getBoolean("wz_video_enabled", mWZBroadcastConfig.isVideoEnabled());
                        if (videoEnabled && !mWZCameraView.isPreviewing()) {
                            mWZCameraView.startPreview();
                        }
                        else if (!videoEnabled && mWZCameraView.isPreviewing()) {
                            mWZCameraView.setVideoBackgroundColor(WOWZColor.BLACK);
                        }
                        mWZCameraView.clearView();
                        mWZCameraView.stopPreview();

                    }
                }
            };

            sharedPrefs.registerOnSharedPreferenceChangeListener(mPrefsChangeListener);

            final CameraActivityBase ref = this;
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mWZBroadcastConfig.isVideoEnabled() && !mWZCameraView.isPreviewing()) {
                        mWZCameraView.startPreview(getBroadcastConfig(), ref);

                    } else {
                        mWZCameraView.stopPreview();
                        Toast.makeText(ref, "The video stream is currently turned off", Toast.LENGTH_LONG).show();
                    }
                }
            }, 300);
        }
        syncUIControlState();
    }

    @Override
    public void onWZCameraPreviewStarted(WOWZCamera wzCamera, WOWZSize wzSize, int i) {
        // Briefly display the video configuration
        Toast.makeText(this, getBroadcastConfig().getLabel(true, true, false, true), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onWZCameraPreviewStopped(int cameraId) {
    }

    @Override
    public void onWZCameraPreviewError(WOWZCamera wzCamera, WOWZError wzError) {
        displayErrorDialog(wzError);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mPrefsChangeListener != null) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(mPrefsChangeListener);
        }

        if (mWZCameraView != null && mWZCameraView.isPreviewing()) {
            mWZCameraView.stopPreview();
        }
    }

    /**
     * WOWZStatusCallback interface methods
     */
    @Override
    public void onWZStatus(final WOWZStatus goCoderStatus) {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (goCoderStatus.isRunning()) {
                    // Keep the screen on while we are broadcasting
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    // Since we have successfully opened up the server connection, store the connection info for auto complete
                    GoCoderSDKPrefs.storeHostConfig(PreferenceManager.getDefaultSharedPreferences(CameraActivityBase.this), mWZBroadcastConfig);
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
    public void onWZError(final WOWZStatus goCoderStatus) {
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
    public void onToggleBroadcast(View v, WOWZBroadcastStatusCallback callback) {
        if (getBroadcast() == null) return;

        if (getBroadcast().getStatus().isIdle()) {
            if (!mWZBroadcastConfig.isVideoEnabled() && !mWZBroadcastConfig.isAudioEnabled()) {
                Toast.makeText(this, "Unable to publish if both audio and video are disabled", Toast.LENGTH_LONG).show();
            }
            else{

                WOWZLog.debug("Scale Mode: -> "+mWZCameraView.getScaleMode());

                if(!mWZBroadcastConfig.isAudioEnabled()){
                    Toast.makeText(this, "The audio stream is currently turned off", Toast.LENGTH_LONG).show();
                }

                if (!mWZBroadcastConfig.isVideoEnabled()) {
                    Toast.makeText(this, "The video stream is currently turned off", Toast.LENGTH_LONG).show();
                }
                WOWZStreamingError configError = startBroadcast(callback);
                if (configError != null) {
                    if (mStatusView != null)
                        mStatusView.setErrorMessage(configError.getErrorDescription());
                }
            }
        } else {
            endBroadcast();
        }
    }

    private FragmentManager.OnBackStackChangedListener backStackListener =  new FragmentManager.OnBackStackChangedListener() {

        @Override
        public void onBackStackChanged() {
            Handler mainHandler = new Handler(Looper.getMainLooper());

            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mWZBroadcastConfig.isVideoEnabled() ) {
                        // Start the camera preview display
                        mWZCameraView.stopPreview();
                        mWZCameraView.startPreview();
                    } else {
                        mWZCameraView.stopPreview();
                    }
                }
            };
            mainHandler.post(myRunnable);
        }
    };

    private GoCoderSDKPrefs.PrefsFragment prefsFragment;
    private void showSettings(View v){
        // Display the prefs fragment

        WOWZLog.debug("*** getOriginalFrameSizes showSettings1");
        prefsFragment.setActiveCamera(mWZCameraView != null ? mWZCameraView.getCamera() : null);

        WOWZLog.debug("*** getOriginalFrameSizes showSettings2");
        getFragmentManager().addOnBackStackChangedListener(backStackListener);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, prefsFragment)
                .addToBackStack(null)
                .show(prefsFragment)
                .commit();
        WOWZLog.debug("*** getOriginalFrameSizes showSettings3");
    }



    /**
     * Click handler for the settings button
     */
    public void onSettings(View v) {
        WOWZLog.debug("*** getOriginalFrameSizes Do we have permission?");
        if(this.hasDevicePermissionToAccess(Manifest.permission.CAMERA) && this.hasDevicePermissionToAccess(Manifest.permission.RECORD_AUDIO)) {
            WOWZLog.debug("*** getOriginalFrameSizes Do we have permission = yes");
            this.showSettings(v);
        }
        else{
            Toast.makeText(this, "You must enable audio / video access to update the settings", Toast.LENGTH_LONG).show();
        }
    }

    protected void initGoCoderDevices() {
        if (sGoCoderSDK != null) {
            boolean videoIsInitialized = false;
            boolean audioIsInitialized = false;

            // Initialize the camera preview
            if(this.hasDevicePermissionToAccess(Manifest.permission.CAMERA)) {
                if (mWZCameraView != null) {
                    WOWZCamera availableCameras[] = mWZCameraView.getCameras();
                    // Ensure we can access to at least one camera
                    if (availableCameras.length > 0) {
                        // Set the video broadcaster in the broadcast config
                        getBroadcastConfig().setVideoBroadcaster(mWZCameraView);
                        videoIsInitialized = true;


                        (new Thread(){
                            public void run(){
                                WOWZLog.debug("*** getOriginalFrameSizes - Get original frame size : ");
                                prefsFragment.setActiveCamera(mWZCameraView != null ? mWZCameraView.getCamera() : null);
                                getFragmentManager().beginTransaction().replace(android.R.id.content, prefsFragment).hide(prefsFragment).commit() ;
                            }
                        }).start();

                    } else {
                        mStatusView.setErrorMessage("Could not detect or gain access to any cameras on this device");
                        getBroadcastConfig().setVideoEnabled(false);
                    }
                }
            }

            if(this.hasDevicePermissionToAccess(Manifest.permission.RECORD_AUDIO)) {
                // Initialize the audio input device interface
                mWZAudioDevice = new WOWZAudioDevice();

                // Set the audio broadcaster in the broadcast config
                getBroadcastConfig().setAudioBroadcaster(mWZAudioDevice);
                audioIsInitialized = true;
            }

            if(videoIsInitialized && audioIsInitialized)
                mDevicesInitialized = true;
        }
    }

    protected void initUIControls() {
        // Initialize the UI controls
        mBtnBroadcast       = findViewById(R.id.ic_broadcast);
        mBtnSettings        = findViewById(R.id.ic_settings);
        mStatusView         = findViewById(R.id.statusView);

        // The GoCoder SDK camera view
        mWZCameraView = findViewById(R.id.cameraPreview);

        mUIInitialized = true;

        if (sGoCoderSDK == null && mStatusView != null) {
            WOWZError mError = WowzaGoCoder.getLastError();
            if (mError!=null)
                mStatusView.setErrorMessage(mError.getErrorDescription());
        }
    }

    protected boolean syncUIControlState() {
        boolean disableControls = (getBroadcast() == null ||
                !(getBroadcast().getStatus().isIdle() ||
                        getBroadcast().getStatus().isBroadcasting()));
        boolean isStreaming = (getBroadcast() != null && getBroadcast().getStatus().isBroadcasting());


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

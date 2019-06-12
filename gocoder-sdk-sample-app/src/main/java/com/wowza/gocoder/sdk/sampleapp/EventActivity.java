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
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.android.opengl.WOWZGLES;
import com.wowza.gocoder.sdk.api.data.WOWZDataEvent;
import com.wowza.gocoder.sdk.api.data.WOWZDataMap;
import com.wowza.gocoder.sdk.api.data.WOWZDataScope;
import com.wowza.gocoder.sdk.api.devices.WOWZCamera;
import com.wowza.gocoder.sdk.api.devices.WOWZCameraView;
import com.wowza.gocoder.sdk.api.geometry.WOWZSize;
import com.wowza.gocoder.sdk.api.logging.WOWZLog;
import com.wowza.gocoder.sdk.api.render.WOWZRenderAPI;
import com.wowza.gocoder.sdk.api.status.WOWZState;
import com.wowza.gocoder.sdk.sampleapp.ui.MultiStateButton;
import com.wowza.gocoder.sdk.sampleapp.ui.TimerView;

import java.util.UUID;

public class EventActivity extends CameraActivityBase {
    private final static String TAG = EventActivity.class.getSimpleName();

    // UI controls
    protected MultiStateButton      mBtnSwitchCamera  = null;
    protected MultiStateButton      mBtnTorch         = null;
    protected MultiStateButton      mBtnPing          = null;
    protected TimerView             mTimerView        = null;
    private WOWZCameraView mWZCameraView   = null;
    private boolean isSendingBeacon=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        mRequiredPermissions = new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };

        // Initialize the UI controls
        mBtnTorch           = findViewById(R.id.ic_torch);
        mBtnSwitchCamera    = findViewById(R.id.ic_switch_camera);
        mBtnPing            = findViewById(R.id.ic_ping);
        mTimerView          = findViewById(R.id.txtTimer);


        if (mWZBroadcast != null) {
            mWZCameraView = findViewById(R.id.cameraPreview);
            WOWZRenderAPI.VideoFrameListener videoFrameListener = new WOWZRenderAPI.VideoFrameListener() {

                @Override
                public boolean isWZVideoFrameListenerActive() {
                    return mWZBroadcast.getStatus().getState()== WOWZState.RUNNING;
                }

                @Override
                public void onWZVideoFrameListenerInit(WOWZGLES.EglEnv eglEnv) {
                    // nothing needed for this example
                }

                @Override
                public void onWZVideoFrameListenerFrameAvailable(WOWZGLES.EglEnv eglEnv, WOWZSize frameSize, int frameRotation, long timecodeNanos) {

                    long ms = timecodeNanos/1000000L;
                    long seconds = timecodeNanos/1000000000L;
                    long mod = seconds % 15L; // per spec, every 15 seconds

                    if(mod == 0 ){
                        if(!isSendingBeacon) {
                            isSendingBeacon = true;

                            WOWZLog.debug("ENCODER", "Timecode: "+ ms +" (seconds: "+seconds+" = "+mod+")");

                            long timestamp = new java.util.Date().getTime(); 
                            String  guid = UUID.randomUUID().toString();
                            WOWZDataMap dataEventParams = new WOWZDataMap();
                            dataEventParams.put("timestamp", String.valueOf(timestamp));
                            dataEventParams.put("stream", mWZBroadcastConfig.getStreamName());
                            dataEventParams.put("guid", guid);
                            dataEventParams.put("version", "1");
                            dataEventParams.put("source", "gocoder");
                            dataEventParams.put("sourceVersion", BuildConfig.VERSION_NAME);

                            WOWZLog.debug("ENCODER", "Sending beacon with :: " + dataEventParams.toString());

                            mWZBroadcast.sendDataEvent(WOWZDataScope.STREAM, "onWowzaLatencyBeacon", dataEventParams);
                        }
                    }
                    else{
                        isSendingBeacon = false;
                    }
                }

                @Override
                public void onWZVideoFrameListenerRelease(WOWZGLES.EglEnv eglEnv) {
                    // nothing needed for this example
                }
            };
            mWZCameraView.registerFrameListener(videoFrameListener);

            mWZBroadcast.registerDataEventListener("onClientConnected", new WOWZDataEvent.EventListener() {
                @Override
                public WOWZDataMap onWZDataEvent(String eventName, WOWZDataMap eventParams) {
                    WOWZLog.info(TAG, "onClientConnected data event received:\n" + eventParams.toString(true));

                    final String result = "A client connected with the IP address " + eventParams.get("clientIp");
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(EventActivity.this, result, Toast.LENGTH_LONG).show();
                        }
                    });

                    // this demonstrates how to return a function result back to the original Wowza Streaming Engine
                    // function call request
                    WOWZDataMap functionResult = new WOWZDataMap();
                    functionResult.put("greeting", "Hello New Client!");

                    return functionResult;
                }
            });

            mWZBroadcast.registerDataEventListener("onClientDisconnected", new WOWZDataEvent.EventListener() {
                @Override
                public WOWZDataMap onWZDataEvent(String eventName, WOWZDataMap eventParams) {
                    WOWZLog.info(TAG, "onClientDisconnected data event received:\n" + eventParams.toString(true));

                    final String result = "A client with the IP address " +  eventParams.get("clientIp") + " disconnected";
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(EventActivity.this, result, Toast.LENGTH_LONG).show();
                        }
                    });

                    return null;
                }
            });

        }
    }

    /**
     * Click handler for the ping button
     */
    public void onPing(View v) {
        //
        // Sending an event to a server module method (with a result callback)
        //
        if (mWZBroadcast != null && mWZBroadcast.getStatus().isRunning()) {
            mBtnPing.setEnabled(false);

            mWZBroadcast.sendPingRequest( new WOWZDataEvent.ResultCallback() {
                @Override
                public void onWZDataEventResult(final WOWZDataMap resultParams, boolean isError) {
                    if(resultParams!=null) {
                        final String result = isError ? "Ping attempt failed (" + resultParams.get("code").toString() + ")" : "SendPingRequest time: " + resultParams.get("responseTime") + "ms";
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(EventActivity.this, result, Toast.LENGTH_LONG).show();
                                mBtnPing.setEnabled(true);
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //
        // Sending an event to a server module method (without a result callback)
        //
        // If a broadcast is active and the user presses on the screen,
        // send a client data event to the server with the coordinates
        // and time
        //
        if (event.getAction() == MotionEvent.ACTION_DOWN &&
                mWZBroadcast != null && mWZBroadcast.getStatus().isRunning()) {

            String  guid = UUID.randomUUID().toString();
            long timestamp = new java.util.Date().getTime();
            WOWZDataMap dataEventParams = new WOWZDataMap();

            dataEventParams.put("text",event.getX() +" x "+ event.getY());
            dataEventParams.put("timestamp", String.valueOf(timestamp));
            dataEventParams.put("stream", mWZBroadcastConfig.getStreamName());
            dataEventParams.put("trackId", "99.0");
            dataEventParams.put("guid", guid);
            dataEventParams.put("version", "1.0");
            dataEventParams.put("source", "encoder");
            dataEventParams.put("sourceId", "GoCoder");
            dataEventParams.put("sourceVersion", BuildConfig.VERSION_NAME);
            dataEventParams.put("occurred", event.getEventTime());

            mWZBroadcast.sendDataEvent(WOWZDataScope.STREAM, "onScreenPress", dataEventParams);
            Toast.makeText(this, "onScreenPress() event sent to server stream", Toast.LENGTH_LONG).show();

            return true;
        } else
            return super.onTouchEvent(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //
        // Sending an event to all stream subscribers
        //
        // If a broadcast is active and the device orientation changes,
        // send a stream data event containing the device orientation
        // and rotation
        //
        if (mWZBroadcast != null && mWZBroadcast.getStatus().isRunning()) {

            WOWZDataMap dataEventParams = new WOWZDataMap();
            dataEventParams.put("deviceOrientation",
                    newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? "landscape" : "portrait");

            Display display = ((WindowManager)
                    getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int displayRotation = display.getRotation();

            switch (displayRotation) {
                case Surface.ROTATION_0:
                    dataEventParams.put("deviceRotation", 0);
                    break;
                case Surface.ROTATION_90:
                    dataEventParams.put("deviceRotation", 90);
                    break;
                case Surface.ROTATION_180:
                    dataEventParams.put("deviceRotation", 180);
                    break;
                case Surface.ROTATION_270:
                    dataEventParams.put("deviceRotation", 270);
                    break;
            }

            mWZBroadcast.sendDataEvent(WOWZDataScope.STREAM, "onDeviceOrientation", dataEventParams);
            Toast.makeText(this, "onDeviceOrientation() event sent to stream subscribers", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Click handler for the switch camera button
     */
    public void onSwitchCamera(View v) {
        if (mWZCameraView == null) return;

        mBtnTorch.setState(false);
        mBtnTorch.setEnabled(false);

        WOWZCamera newCamera = mWZCameraView.switchCamera();
        if (newCamera != null) {
            boolean hasTorch = newCamera.hasCapability(WOWZCamera.TORCH);
            if (hasTorch) {
                mBtnTorch.setState(newCamera.isTorchOn());
                mBtnTorch.setEnabled(true);
            }
        }
    }

    /**
     * Click handler for the torch/flashlight button
     */
    public void onToggleTorch(View v) {
        if (mWZCameraView == null) return;

        WOWZCamera activeCamera = mWZCameraView.getCamera();
        activeCamera.setTorchOn(mBtnTorch.toggleState());
    }

    /**
     * Click handler for the Settings button
     */
    public void onSettings(View v) {
        super.onToggleBroadcast(v);
    }

    /**
     * Click handler for the ToggleBroadcast button
     */
    public void onToggleBroadcast(View v) {
        super.onToggleBroadcast(v);
    }

   /**
     * Update the state of the UI controls
     */
    @Override
    protected boolean syncUIControlState() {
        boolean disableControls = super.syncUIControlState();

        if (disableControls) {
            mBtnSwitchCamera.setEnabled(false);
            mBtnTorch.setEnabled(false);
            mBtnPing.setEnabled(false);
        } else {
            boolean isDisplayingVideo = false;
            boolean isStreaming = getBroadcast().getStatus().isRunning();

            if(this.hasDevicePermissionToAccess()){
                isDisplayingVideo = (getBroadcastConfig().isVideoEnabled() && mWZCameraView.getCameras().length > 0);
            }

            mBtnPing.setEnabled(isStreaming);

            if (isDisplayingVideo) {
                WOWZCamera activeCamera = mWZCameraView.getCamera();

                boolean hasTorch = (activeCamera != null && activeCamera.hasCapability(WOWZCamera.TORCH));
                mBtnTorch.setEnabled(hasTorch);
                if (hasTorch) {
                    mBtnTorch.setState(activeCamera.isTorchOn());
                }

                mBtnSwitchCamera.setEnabled(mWZCameraView.getCameras().length > 0);
            } else {
                mBtnSwitchCamera.setEnabled(false);
                mBtnTorch.setEnabled(false);
            }

            if (isStreaming && !mTimerView.isRunning()) {
                mTimerView.startTimer();
            } else if (getBroadcast().getStatus().isIdle() && mTimerView.isRunning()) {
                mTimerView.stopTimer();
            } else if (!isStreaming) {
                mTimerView.setVisibility(View.GONE);
            }
        }

        return disableControls;
    }
}

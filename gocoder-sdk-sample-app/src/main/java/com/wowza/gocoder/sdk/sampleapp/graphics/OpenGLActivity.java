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

package com.wowza.gocoder.sdk.sampleapp.graphics;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;

import com.wowza.gocoder.sdk.api.errors.WOWZStreamingError;
import com.wowza.gocoder.sdk.api.geometry.WOWZSize;
import com.wowza.gocoder.sdk.api.status.WOWZStatus;
import com.wowza.gocoder.sdk.sampleapp.GoCoderSDKActivityBase;
import com.wowza.gocoder.sdk.sampleapp.R;
import com.wowza.gocoder.sdk.sampleapp.config.GoCoderSDKPrefs;
import com.wowza.gocoder.sdk.sampleapp.ui.MultiStateButton;
import com.wowza.gocoder.sdk.sampleapp.ui.StatusView;

public class OpenGLActivity extends GoCoderSDKActivityBase {
    private static final String TAG = OpenGLActivity.class.getSimpleName();

    protected MultiStateButton mBtnBroadcast = null;
    protected MultiStateButton mBtnSettings  = null;
    protected StatusView mStatusView = null;

    private GLSurfaceView mGLSurfaceView = null;
    private OpenGLRenderer mOpenGLRenderer = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opengl);

        // Initialize the UI controls
        mBtnBroadcast       = (MultiStateButton) findViewById(R.id.ic_broadcast);
        mBtnSettings        = (MultiStateButton) findViewById(R.id.ic_settings);
        mStatusView         = (StatusView) findViewById(R.id.statusView);

        // Initialize the OpenGL ES surface view
        mGLSurfaceView = (GLSurfaceView)findViewById(R.id.gl_surface_view);
        mGLSurfaceView.setEGLContextClientVersion(2);

        mOpenGLRenderer = new OpenGLRenderer();
        mGLSurfaceView.setRenderer(mOpenGLRenderer);
    }

    @Override
    protected void onResume() {
        syncUIControlState();

        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override protected void onPause()
    {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        mGLSurfaceView.onPause();
    }

    /**
     * Click handler for the broadcast button
     */
    public void onToggleBroadcast(View v) {
        if (getBroadcast() == null) return;

        if (getBroadcast().getStatus().isIdle()) {
            // Set the video stream broadcasting source to the WOWZGLBroadcaster created by the OpenGLRenderer
            mWZBroadcastConfig.setVideoBroadcaster(mOpenGLRenderer.getGLBroadcaster());

            // Get the current surface size to use as the video stream frame size
            WOWZSize openGLSurfaceSize = mOpenGLRenderer.getSurfaceSize();
            mWZBroadcastConfig.setVideoFrameSize(openGLSurfaceSize);
            mWZBroadcastConfig.getVideoSourceConfig().setVideoFrameSize(openGLSurfaceSize);

            // Disable audio streaming
            mWZBroadcastConfig.setAudioEnabled(false);

            WOWZStreamingError configError = startBroadcast();
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
        // Display the prefs fragment
        GoCoderSDKPrefs.PrefsFragment prefsFragment = new GoCoderSDKPrefs.PrefsFragment();
        prefsFragment.setFixedVideoSource(true);
        prefsFragment.setShowAudioPrefs(false);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, prefsFragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * WOWZStatusCallback interface methods
     */
    @Override
    public void onWZStatus(final WOWZStatus goCoderStatus) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (goCoderStatus.isRunning()) {  // The stream has started
                    // Keep the screen on while we are broadcasting
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    // Since we have successfully opened up the server connection, store the connection info for auto complete
                    GoCoderSDKPrefs.storeHostConfig(PreferenceManager.getDefaultSharedPreferences(OpenGLActivity.this), mWZBroadcastConfig);

                    // Tell the renderer that streaming has started
                    mOpenGLRenderer.onStreamStart();

                } else if (goCoderStatus.isIdle()) { // The stream has stopped
                    // Clear the "keep screen on" flag
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    // Tell the renderer that streaming has stopped
                    mOpenGLRenderer.onStreamEnd();
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

    protected boolean syncUIControlState() {
        boolean disableControls = (getBroadcast() == null ||
                !(getBroadcast().getStatus().isIdle() ||
                        getBroadcast().getStatus().isRunning()));
        boolean isStreaming = (getBroadcast() != null && getBroadcast().getStatus().isRunning());

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

/**
 *  This code and all components (c) Copyright 2015-2016, Wowza Media Systems, LLC. All rights reserved.
 *  This code is licensed pursuant to the BSD 3-Clause License.
 */
package com.wowza.gocoder.sdk.sampleapp.mp4;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;

import com.wowza.gocoder.sdk.api.WowzaGoCoder;
import com.wowza.gocoder.sdk.api.broadcast.WZBroadcast;
import com.wowza.gocoder.sdk.api.broadcast.WZBroadcastConfig;
import com.wowza.gocoder.sdk.api.configuration.WZMediaConfig;
import com.wowza.gocoder.sdk.api.errors.WZStreamingError;
import com.wowza.gocoder.sdk.api.logging.WZLog;
import com.wowza.gocoder.sdk.api.mp4.WZMP4Broadcaster;
import com.wowza.gocoder.sdk.api.mp4.WZMP4Util;
import com.wowza.gocoder.sdk.api.status.WZStatus;
import com.wowza.gocoder.sdk.api.status.WZStatusCallback;
import com.wowza.gocoder.sdk.sampleapp.R;
import com.wowza.gocoder.sdk.sampleapp.ui.StatusView;
import com.wowza.gocoder.sdk.sampleapp.config.ConfigPrefs;
import com.wowza.gocoder.sdk.sampleapp.config.ConfigPrefsActivity;
import com.wowza.gocoder.sdk.sampleapp.ui.ControlButton;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;

/**
 * This activity demonstrates the use of an alternate video input source
 * by streaming the frames from an MP4 file
 */
public class MP4BroadcastActivity extends Activity
    implements WZStatusCallback {

    private final static String TAG = MP4BroadcastActivity.class.getSimpleName();

    final private static String SDK_SAMPLE_APP_LICENSE_KEY = "GSDK-CA41-0001-E32F-0CF1-93EC";

    private static final int VIDEO_SELECTED_RESULT_CODE = 1;

    // UI controls
    private ControlButton       mBtnBroadcast;
    private ControlButton       mBtnSettings;
    private ControlButton       mBtnFileSelect;
    private ControlButton       mBtnLoop;

    private VideoView           mVideoView;
    private StatusView          mStatusView;

    private Uri                 mMP4FileUri;
    private WZMP4Broadcaster    mMP4Broadcaster;

    private WZBroadcast         mBroadcast;
    private WZBroadcastConfig   mBroadcastConfig;

    private MediaPlayer         mMediaPlayer;
    private boolean             mLooping;

    // GoCoder SDK top level interface
    private static WowzaGoCoder sGoCoder = null;

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private static final int PERMISSIONS_REQUEST_CODE = 0x1;
    private boolean mPermissionsGranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        mMediaPlayer = null;
        mLooping = true;
        mMP4FileUri = null;
        mPermissionsGranted = false;
        mBroadcast = null;

        // Initialize the UI controls
        setContentView(R.layout.activity_mp4_broadcast);

        mBtnBroadcast       = new ControlButton(this, R.id.ic_broadcast, false, false, R.drawable.ic_pause, R.drawable.ic_play);
        mBtnFileSelect      = new ControlButton(this, R.id.ic_videos, true);
        mBtnSettings        = new ControlButton(this, R.id.ic_settings, true);
        mBtnLoop            = new ControlButton(this, R.id.ic_loop, true, true, R.drawable.ic_refresh, R.drawable.ic_refresh_off);

        mVideoView          = (VideoView) findViewById(R.id.vwVideoPlayer);
        mStatusView         = (StatusView) findViewById(R.id.statusView);

        // Initialize the GoCoder SDK
        if (sGoCoder == null) {
            // Enable detailed logging from the GoCoder SDK
            WZLog.LOGGING_ENABLED = true;

            // Initialize the GoCoder SDK
            sGoCoder = WowzaGoCoder.init(this, SDK_SAMPLE_APP_LICENSE_KEY);
            if (sGoCoder == null) {
                mStatusView.setErrorMessage(WowzaGoCoder.getLastError().getErrorDescription());
                return;
            }

            WZLog.info("GoCoder SDK version number = " + WowzaGoCoder.SDK_VERSION);
            WZLog.info("Platform information = " + WowzaGoCoder.PLATFORM_INFO);
        }

        if (sGoCoder != null) {
            // Create a new MP4 broadcaster instance
            mMP4Broadcaster = new WZMP4Broadcaster();

            // Register the MP4 broadcaster as the video source in the broadcast config
            mBroadcastConfig = new WZBroadcastConfig();
            mBroadcastConfig.setVideoBroadcaster(mMP4Broadcaster);
            mBroadcastConfig.setAudioEnabled(false);

            // Crceate the primary GoCoder SDK broadcaster
            mBroadcast = new WZBroadcast();
        }

        // Callback invoked when the video player is ready
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mediaPlayer) {
                mMediaPlayer = mediaPlayer;
                mediaPlayer.setLooping(mLooping);
                mediaPlayer.setVolume(0f, 0f);
                mediaPlayer.seekTo(0);
            }
        });

        // Callback invoked when the video has completed playing
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mMediaPlayer = null;
                if (mBroadcast != null && mBroadcast.getBroadcastStatus().isRunning()) {
                    mBroadcast.endBroadcast(MP4BroadcastActivity.this);
                    updateUIControls();
                }
            }
        });
    }

    /**
     * Android Activity class methods
     */

    @Override
    protected void onResume() {
        super.onResume();

        if (mBroadcastConfig != null) {
            ConfigPrefs.updateConfigFromPrefs(PreferenceManager.getDefaultSharedPreferences(this), mBroadcastConfig);
            mBroadcastConfig.setAudioEnabled(false);
        }

        updateUIControls();

        // Ensure we have the permissions need
        mPermissionsGranted = WowzaGoCoder.hasPermissions(this, REQUIRED_PERMISSIONS);
        if (!mPermissionsGranted) {
            ActivityCompat.requestPermissions(this,
                    REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        }
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

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

        // Stop any active broadcast
        if (mBroadcast != null && mBroadcast.getBroadcastStatus().isRunning())
            mBroadcast.endBroadcast();
    }

    /**
     * Click handler for the video selector button
     */
    public void onSelectMedia(View v) {
        if (!mPermissionsGranted) {
            mStatusView.setErrorMessage("The application has not been granted permission to read from external storage");
            ActivityCompat.requestPermissions(this,
                    REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        } else {
            selectVideoFile();
        }
    }

    // Setup the video file selection dialog
    private void selectVideoFile() {
        mBtnBroadcast.setEnabled(false);
        mBtnSettings.setEnabled(false);
        mBtnFileSelect.setEnabled(false);

        Intent mediaChooser = new Intent(Intent.ACTION_GET_CONTENT);
        mediaChooser.setType("video/*");
        mediaChooser.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(mediaChooser, VIDEO_SELECTED_RESULT_CODE);
    }

    @Override
    // Invoked once an MP4 file has been selected
    protected void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        mBtnFileSelect.setEnabled(true);
        mBtnSettings.setEnabled(false);

        switch(requestCode){
            case VIDEO_SELECTED_RESULT_CODE:
                if (resultCode==RESULT_OK) {
                    Uri fileUri = returnIntent.getData();
                    String mimeType = getContentResolver().getType(fileUri);

                    if (!mimeType.equals("video/mp4")) {
                        mStatusView.setErrorMessage("The video selected is not an MP4 file");
                    } else {
                        setVideoFile(fileUri);
                        updateUIControls();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, returnIntent);
    }

    // Set the MP4 file to broadcast
    private void setVideoFile(Uri fileUri) {
        FileDescriptor fd = getFD(fileUri);

        if (fd != null) {
            mMP4Broadcaster.setFileDescriptor(fd);

            WZMediaConfig fileConfig = WZMP4Util.getFileConfig(fd);
            if (fileConfig != null) {
                mMP4FileUri = fileUri;

                mBroadcastConfig.set(fileConfig);
                mBroadcastConfig.setAudioEnabled(false);

                findViewById(R.id.vwHelp).setVisibility(View.INVISIBLE);

                mVideoView.setVideoURI(mMP4FileUri);
                mVideoView.setVisibility(View.VISIBLE);
            } else {
                mVideoView.setVisibility(View.INVISIBLE);
                findViewById(R.id.vwHelp).setVisibility(View.VISIBLE);
                mStatusView.setErrorMessage("The format of the selected file could not be determined");
            }
        } else {
            mStatusView.setErrorMessage("Could not open file");
        }
    }

    // Get a file decriptor for the file selected
    private FileDescriptor getFD(Uri fileUri) {
        FileDescriptor fd;
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(fileUri, "r");
            fd = pfd.getFileDescriptor();
        } catch (FileNotFoundException e) {
            fd = null;
        }

        return fd;
    }

    /**
     * Click handler for the broadcast button
     */
    public void onToggleBroadcast(View v) {
        if (mMP4FileUri == null) {
            mStatusView.setErrorMessage("A video file has not been selected");
            return;
        }

        boolean startBroadcast = !mBtnBroadcast.isStateOn();

        if (startBroadcast) {
            // rewind the video player
            mVideoView.seekTo(0);
            mVideoView.start();

            // Tell the MP4 broadcaster which file to use
            mMP4Broadcaster.setFileDescriptor(getFD(mMP4FileUri));

            // Validate the broadcast config
            WZStreamingError configValidationError = mBroadcastConfig.validateForBroadcast();
            if (configValidationError != null) {
                mStatusView.setErrorMessage(configValidationError.getErrorDescription());
            } else {
                mBroadcast.startBroadcast(mBroadcastConfig, this);
            }

        } else {
            // Stop the broadcast and video player
            if (mVideoView.isPlaying()) {
                mVideoView.pause();
            }

            mBroadcast.endBroadcast(this);
        }
    }

    /**
     * Click handler for the settings button
     */
    public void onSettings(View v) {
        Intent intent = new Intent(this, ConfigPrefsActivity.class);
        intent.putExtra(ConfigPrefs.PREFS_TYPE, ConfigPrefs.VIDEO_AND_CONNECTION);
        intent.putExtra(ConfigPrefs.FIXED_FRAME_SIZE, true);
        intent.putExtra(ConfigPrefs.FIXED_FRAME_RATE, true);
        startActivity(intent);
    }

    /**
     * Click handler for the loop button
     */
    public void onLoop(View v) {
        mLooping = mBtnLoop.toggleState();
        mMediaPlayer.setLooping(mLooping);
    }

    /**
     * WZStatusCallback interface methods
     */

    @Override
    public void onWZStatus(final WZStatus goCoderStatus) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mStatusView.setStatus(goCoderStatus);
                updateUIControls();
            }
        });
    }

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
     * Update the state of the UI controls
     */
    private void updateUIControls() {
        boolean disableControls = (mBroadcast == null ||
                !(mBroadcast.getBroadcastStatus().isIdle() || mBroadcast.getBroadcastStatus().isRunning()));

        if (disableControls) {
            mBtnBroadcast.setEnabled(false);
            mBtnSettings.setEnabled(false);
            mBtnLoop.setEnabled(false);
            mBtnFileSelect.setEnabled(false);
        } else {
            boolean isStreaming = mBroadcast.getBroadcastStatus().isRunning();
            mBtnBroadcast.setStateOn(isStreaming);
            mBtnBroadcast.setEnabled(mMP4FileUri != null);

            mBtnSettings.setEnabled(!isStreaming);
            mBtnLoop.setVisible(!isStreaming);
            mBtnFileSelect.setEnabled(!isStreaming);
        }
    }

}

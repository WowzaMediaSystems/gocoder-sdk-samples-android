/**
 *
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

package com.wowza.gocoder.sdk.sampleapp.mp4;

import android.Manifest;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.WindowManager;
import android.widget.VideoView;

import com.wowza.gocoder.sdk.api.WowzaGoCoder;
import com.wowza.gocoder.sdk.api.configuration.WZMediaConfig;
import com.wowza.gocoder.sdk.api.errors.WZStreamingError;
import com.wowza.gocoder.sdk.api.mp4.WZMP4Broadcaster;
import com.wowza.gocoder.sdk.api.mp4.WZMP4Util;
import com.wowza.gocoder.sdk.api.status.WZState;
import com.wowza.gocoder.sdk.api.status.WZStatus;
import com.wowza.gocoder.sdk.sampleapp.GoCoderSDKActivityBase;
import com.wowza.gocoder.sdk.sampleapp.R;
import com.wowza.gocoder.sdk.sampleapp.ui.MultiStateButton;
import com.wowza.gocoder.sdk.sampleapp.ui.StatusView;
import com.wowza.gocoder.sdk.sampleapp.config.ConfigPrefs;
import com.wowza.gocoder.sdk.sampleapp.config.ConfigPrefsActivity;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;

public class MP4BroadcastActivity extends GoCoderSDKActivityBase {
    final private static String TAG = MP4BroadcastActivity.class.getSimpleName();

    final private static int VIDEO_SELECTED_RESULT_CODE = 1;

    // UI controls
    private MultiStateButton    mBtnFileSelect;
    private MultiStateButton    mBtnLoop;

    protected MultiStateButton  mBtnBroadcast     = null;
    protected MultiStateButton  mBtnSettings      = null;

    private VideoView           mVideoView;
    private StatusView          mStatusView;

    private Uri                 mMP4FileUri;
    private WZMP4Broadcaster    mMP4Broadcaster;

    private MediaPlayer         mMediaPlayer;
    private boolean             mLooping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp4_broadcast);

        mRequiredPermissions = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        mMediaPlayer        = null;
        mLooping            = true;
        mMP4FileUri         = null;

        mBtnBroadcast       = (MultiStateButton) findViewById(R.id.ic_broadcast);
        mBtnSettings        = (MultiStateButton) findViewById(R.id.ic_settings);

        mBtnFileSelect      = (MultiStateButton) findViewById(R.id.ic_videos);
        mBtnLoop            = (MultiStateButton) findViewById(R.id.ic_loop);

        mVideoView          = (VideoView) findViewById(R.id.vwVideoPlayer);
        mStatusView         = (StatusView) findViewById(R.id.statusView);

        if (sGoCoderSDK != null) {
            mMP4Broadcaster = new WZMP4Broadcaster();
            mWZBroadcastConfig.setVideoBroadcaster(mMP4Broadcaster);
            mWZBroadcastConfig.setAudioEnabled(false); // audio not yet supported

            mBtnLoop.setState(mLooping);

            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mMediaPlayer = mediaPlayer;
                    mediaPlayer.setLooping(mLooping);
                    mediaPlayer.setVolume(0f, 0f);
                    mediaPlayer.seekTo(0);
                }
            });

            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mMediaPlayer = null;
                }
            });
        } else if (mStatusView != null) {
            mStatusView.setErrorMessage(WowzaGoCoder.getLastError().getErrorDescription());
        }
    }

    /**
     * Android Activity class methods
     */

    @Override
    protected void onResume() {
        super.onResume();
        syncUIControlState();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Click handler for the video selector button
     */
    public void onSelectMedia(View v) {
        if (!mPermissionsGranted) {
            mStatusView.setErrorMessage("The application has not been granted permission to read from external storage");
        } else {
            selectVideoFile();
        }
    }

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
    protected void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        mBtnFileSelect.setEnabled(true);
        mBtnSettings.setEnabled(false);

        switch(requestCode){
            case VIDEO_SELECTED_RESULT_CODE:
                if (resultCode==RESULT_OK) {
                    Uri fileUri = returnIntent.getData();
                    String mimeType = getContentResolver().getType(fileUri);

                    if (mimeType != null && !mimeType.equals("video/mp4")) {
                        mStatusView.setErrorMessage("The video selected is not an MP4 file");
                    } else {
                        setVideoFile(fileUri);
                        syncUIControlState();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, returnIntent);
    }

    private void setVideoFile(Uri fileUri) {
        FileDescriptor fd = getFD(fileUri);

        if (fd != null) {
            mMP4Broadcaster.setFileDescriptor(fd);

            WZMediaConfig fileConfig = WZMP4Util.getFileConfig(fd);
            if (fileConfig != null) {
                fileConfig.setAudioEnabled(false);
                mMP4FileUri = fileUri;

                mWZBroadcastConfig.set(fileConfig);

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

    private FileDescriptor getFD(Uri fileUri) {
        FileDescriptor fd;
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(fileUri, "r");
            if (pfd != null)
                fd = pfd.getFileDescriptor();
            else
                return null;
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
            mStatusView.setErrorMessage("An MP4 file has not been selected");
            return;
        }

        if (mWZBroadcast.getStatus().isIdle()) {
            mMP4Broadcaster.setFileDescriptor(getFD(mMP4FileUri));
            mWZBroadcastConfig.setAudioEnabled(false);

            WZStreamingError configValidationError = mWZBroadcastConfig.validateForBroadcast();
            if (configValidationError != null) {
                mStatusView.setErrorMessage(configValidationError.getErrorDescription());
            } else {
                mWZBroadcast.startBroadcast(mWZBroadcastConfig, this);
            }
        } else if (mWZBroadcast.getStatus().isRunning()) {
            if (mVideoView.isPlaying()) {
                mVideoView.pause();
            }
            mWZBroadcast.endBroadcast(this);
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
        mMP4Broadcaster.setLooping(mLooping);
        if (mMediaPlayer != null)
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
                switch(goCoderStatus.getState()) {
                    case WZState.IDLE:
                        // Clear the "keep screen on" flag
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                            mMediaPlayer.stop();
                        }
                        break;
                    case WZState.RUNNING:
                        // Keep the screen on while we are broadcasting
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                        mVideoView.seekTo(0);
                        mVideoView.start();
                        break;
                }
                mStatusView.setStatus(goCoderStatus);
                syncUIControlState();
            }
        });
    }

    @Override
    public void onWZError(final WZStatus goCoderStatus) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mStatusView.setStatus(goCoderStatus);
                syncUIControlState();
            }
        });
    }

    /**
     * Update the state of the UI controls
     */
    private void syncUIControlState() {
        boolean disableControls = (mWZBroadcast == null ||
                !(mWZBroadcast.getStatus().isIdle() || mWZBroadcast.getStatus().isRunning()));

        if (disableControls) {
            mBtnBroadcast.setEnabled(false);
            mBtnSettings.setEnabled(false);
            mBtnLoop.setEnabled(false);
            mBtnFileSelect.setEnabled(false);
        } else {
            boolean isStreaming = mWZBroadcast.getStatus().isRunning();
            mBtnBroadcast.setState(isStreaming);
            mBtnBroadcast.setEnabled(mMP4FileUri != null);

            mBtnSettings.setEnabled(!isStreaming);
            mBtnFileSelect.setEnabled(!isStreaming);
        }
    }
}

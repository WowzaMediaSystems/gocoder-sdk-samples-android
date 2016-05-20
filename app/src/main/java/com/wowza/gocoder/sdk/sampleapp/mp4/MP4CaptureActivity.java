/**
 *  This code and all components (c) Copyright 2015-2016, Wowza Media Systems, LLC. All rights reserved.
 *  This code is licensed pursuant to the BSD 3-Clause License.
 */
package com.wowza.gocoder.sdk.sampleapp.mp4;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.wowza.gocoder.sdk.api.logging.WZLog;
import com.wowza.gocoder.sdk.api.mp4.WZMP4Writer;
import com.wowza.gocoder.sdk.api.status.WZStatus;
import com.wowza.gocoder.sdk.sampleapp.CameraActivity;
import com.wowza.gocoder.sdk.sampleapp.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This activity saves an MP4 video file to local storage during a live streaming broadcast
 */
public class MP4CaptureActivity extends CameraActivity {
    private static String TAG = MP4CaptureActivity.class.getSimpleName();

    protected Switch            mSwitchMP4          = null;
    protected WZMP4Writer       mMP4Writer          = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the permission required by this activity
        mRequiredPermissions = new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (sGoCoder != null) {
            mSwitchMP4 = (Switch) findViewById(R.id.swSaveMP4);
            mSwitchMP4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    // Enable or display the MP4 video sink based on the MP4 toggle switch
                    if (isChecked) {
                        mBroadcastConfig.registerVideoSink(mMP4Writer);
                    } else {
                        mBroadcastConfig.unregisterVideoSink(mMP4Writer);
                    }
                }
            });

            mMP4Writer = new WZMP4Writer();
            mBroadcastConfig.registerVideoSink(mMP4Writer);

            mSwitchMP4.setChecked(true);
            mSwitchMP4.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Android Activity lifecycle methods
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Click handler for the broadcast button
     */
    @Override
    public void onToggleBroadcast(View v) {
        if (mBroadcast.getBroadcastStatus().isIdle()) {
            if (mSwitchMP4.isChecked()) {
                // Create the directory in which to store the MP4 file
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                File outputFile = getOutputMediaFile();
                if (outputFile != null)
                    mMP4Writer.setFilePath(outputFile.toString());
                else {
                    mStatusView.setErrorMessage("Could not create or access the directory in which to store the MP");
                    mSwitchMP4.setChecked(false);
                    return;
                }
            }
        } else if (mSwitchMP4.isChecked()) {
            WZLog.debug(TAG, "The MP4 file was stored at " + mMP4Writer.getFilePath());
            mStatusView.showMessage("The MP4 file was stored at " + mMP4Writer.getFilePath());
        }

        mBroadcastConfig.setAudioEnabled(false); // audio support coming soon
        super.onToggleBroadcast(v);
   }

    @Override
    public void onWZStatus(final WZStatus goCoderStatus) {
        super.onWZStatus(goCoderStatus);
    }

    /**
     * Update the state of the UI controls
     */
    @Override
    protected boolean updateUIControls() {
        mSwitchMP4.setVisibility(getBroadcast().getBroadcastStatus().isRunning() ? View.INVISIBLE : View.VISIBLE);

        boolean disableControls = super.updateUIControls();
        mSwitchMP4.setEnabled(!disableControls);

        return disableControls;
    }


    /**
     * Create the MP4 file container
     */
    private File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        //
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "GoCoderSDK");

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                WZLog.warn(TAG, "failed to create the directory in which to store the MP4");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "WOWZA_"+ timeStamp + ".mp4");
    }
}


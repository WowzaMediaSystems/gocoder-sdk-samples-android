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

package com.wowza.gocoder.sdk.sampleapp.mp4;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.wowza.gocoder.sdk.api.logging.WOWZLog;
import com.wowza.gocoder.sdk.api.mp4.WOWZMP4Writer;
import com.wowza.gocoder.sdk.sampleapp.CameraActivity;
import com.wowza.gocoder.sdk.sampleapp.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MP4CaptureActivity extends CameraActivity {
    private final static String TAG = MP4CaptureActivity.class.getSimpleName();

    protected LinearLayout      mMP4Controls        = null;
    protected Switch            mSwitchMP4          = null;
    protected WOWZMP4Writer mMP4Writer          = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRequiredPermissions = new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (sGoCoderSDK != null) {
            mMP4Controls = (LinearLayout) findViewById(R.id.mp4Controls);
            mSwitchMP4 = (Switch) findViewById(R.id.swSaveMP4);
            mSwitchMP4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    if (isChecked) {
                        mWZBroadcastConfig.registerVideoSink(mMP4Writer);
                        mWZBroadcastConfig.registerAudioSink(mMP4Writer);
                    } else {
                        mWZBroadcastConfig.unregisterVideoSink(mMP4Writer);
                        mWZBroadcastConfig.unregisterAudioSink(mMP4Writer);
                    }
                }
            });

            mMP4Writer = new WOWZMP4Writer();
            mWZBroadcastConfig.registerVideoSink(mMP4Writer);
            mWZBroadcastConfig.registerAudioSink(mMP4Writer);

            mSwitchMP4.setChecked(true);
            mMP4Controls.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Click handler for the broadcast button
     */
    @Override
    public void onToggleBroadcast(View v) {
        if (mWZBroadcast.getStatus().isIdle()) {
            if (mSwitchMP4.isChecked()) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                File outputFile = getOutputMediaFile();
                if (outputFile != null) {
                    mMP4Writer.setFilePath(outputFile.toString());
                }
                else {
                    mStatusView.setErrorMessage("Could not create or access the directory in which to store the MP4");
                    mSwitchMP4.setChecked(false);
                    return;
                }
            }
        } else if (mSwitchMP4.isChecked()) {
            WOWZLog.debug(TAG, "The MP4 file was stored at " + mMP4Writer.getFilePath());
            mStatusView.showMessage("The MP4 file was stored at " + mMP4Writer.getFilePath());

        }
        super.onToggleBroadcast(v);
    }

    /**
     * Update the state of the UI controls
     */
    @Override
    protected boolean syncUIControlState() {
        boolean disableControls = super.syncUIControlState();

        if (getBroadcast() != null) {
            mMP4Controls.setVisibility(getBroadcast().getStatus().isRunning() ? View.INVISIBLE : View.VISIBLE);
            mSwitchMP4.setEnabled(!disableControls);
        }

        return disableControls;
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        //
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "GoCoderSDK MP4s");

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                WOWZLog.warn(TAG, "failed to create the directory in which to store the MP4");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator
                + timeStamp + ".mp4");
    }
}


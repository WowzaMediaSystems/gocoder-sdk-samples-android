/*
 *
 * WOWZA MEDIA SYSTEMS, LLC ("Wowza") CONFIDENTIAL
 * Copyright (c) 2005-2016 Wowza Media Systems, LLC, All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property of Wowza Media Systems, LLC.
 * The intellectual and technical concepts contained herein are proprietary to Wowza Media Systems, LLC
 * and may be covered by U.S. and Foreign Patents, patents in process, and are protected by trade secret
 * or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from Wowza Media Systems, LLC. Access to the source code
 * contained herein is hereby forbidden to anyone except current Wowza Media Systems, LLC employees, managers
 * or contractors who have executed Confidentiality and Non-disclosure agreements explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication or disclosure of this
 * source code, which includes information that is confidential and/or proprietary, and is a trade secret, of
 * Wowza Media Systems, LLC. ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF WOWZA MEDIA SYSTEMS, LLC IS
 * STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES. THE RECEIPT OR POSSESSION
 * OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR
 * DISTRIBUTE ITS CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 *
 */

package com.wowza.gocoder.sdk.sampleapp.audio;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.errors.WZStreamingError;
import com.wowza.gocoder.sdk.api.status.WZStatus;
import com.wowza.gocoder.sdk.sampleapp.CameraActivityBase;
import com.wowza.gocoder.sdk.sampleapp.R;
import com.wowza.gocoder.sdk.sampleapp.config.ConfigPrefs;
import com.wowza.gocoder.sdk.sampleapp.ui.AudioLevelMeter;
import com.wowza.gocoder.sdk.sampleapp.ui.MultiStateButton;

/**
 * This activity class demonstrates how to create a class that implements the WZAudioDevice.AudioSampleListener interface
 * to sample PCM audio from the default audio input device. The {@link AudioLevelMeter} class implements the interface and
 * an instance of that class is registered with the {@code WZAudioDevice} API using the registerAudioSampleListener(WZAudioDevice.AudioSampleListener)
 * method.
 */
public class AudioMeterActivity extends CameraActivityBase {

    protected MultiStateButton  mBtnMic                 = null;
    protected AudioLevelMeter   mAudioLevelMeter        = null;
    private boolean             mRestartAudioSampler    = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_meter);

        mRequiredPermissions = new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };

        mBtnMic             = (MultiStateButton) findViewById(R.id.ic_mic);
        mAudioLevelMeter    = (AudioLevelMeter) findViewById(R.id.audioLevelMeter);
    }

    /**
     * Initialize the audio device and audio sampler
     */
    protected void setupAudioDevices() {
        mWZAudioDevice.registerAudioSampleListener(mAudioLevelMeter);

        mBtnMic.setState(true);
        mAudioLevelMeter.setVisibility(View.VISIBLE);
        mWZAudioDevice.startAudioSampler();

        Toast.makeText(this, getString(R.string.audio_meter_help), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mWZAudioDevice != null) {
            setupAudioDevices();
        } else {
            mBtnMic.setEnabled(false);
            mAudioLevelMeter.setVisibility(View.GONE);
        }
    }

    /**
     * Shutdown/release the audio device and audio sampler
     */
    protected void releaseAudioDevices() {
        if (mWZAudioDevice != null) {
            if (mWZAudioDevice.isSamplingAudio())
                mWZAudioDevice.stopAudioSampler();
            mWZAudioDevice.unregisterAudioSampleListener(mAudioLevelMeter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseAudioDevices();
    }

    /**
     * Click handler for the mic/mute button
     */
    public void onToggleMute(View v) {
        mBtnMic.toggleState();

        if (getBroadcast().getStatus().isRunning()) {
            mWZAudioDevice.setAudioPaused(!mBtnMic.isOn());
            Toast.makeText(this, "Audio stream " + (mWZAudioDevice.isAudioPaused() ? "muted" : "enabled"), Toast.LENGTH_SHORT).show();
        } else {
            mAudioLevelMeter.setVisibility(mBtnMic.isOn() ? View.VISIBLE : View.GONE);

            if (mBtnMic.isOn())
                mWZAudioDevice.startAudioSampler();
            else
                mWZAudioDevice.stopAudioSampler();
        }
   }
    @Override
    protected synchronized WZStreamingError startBroadcast() {
        mRestartAudioSampler = (mWZAudioDevice != null && mWZAudioDevice.isSamplingAudio());
        return super.startBroadcast();
    }

    @Override
    public void onWZStatus(final WZStatus goCoderStatus) {
        super.onWZStatus(goCoderStatus);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Restart audio sampler if it was running when broadcast began
                if (goCoderStatus.isIdle() && mRestartAudioSampler) {
                    mRestartAudioSampler = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWZAudioDevice.startAudioSampler();
                            mBtnMic.setState(true);
                            mBtnMic.setEnabled(true);
                        }
                    });
                }
            }
        });
    }

    @Override
    protected boolean syncUIControlState() {
        boolean disableControls = super.syncUIControlState();

        if (disableControls) {
            mBtnMic.setEnabled(false);
        } else if (mWZAudioDevice != null) {
            boolean isStreaming = getBroadcast().getStatus().isRunning();
            boolean isStreamingAudio = (isStreaming && getBroadcastConfig().isAudioEnabled());
            boolean isSamplingAudio = (mRestartAudioSampler || mWZAudioDevice.isSamplingAudio() || isStreamingAudio);

            mBtnMic.setEnabled((!isStreaming)||isStreamingAudio);
            mBtnMic.setState(isSamplingAudio);
            mAudioLevelMeter.setVisibility(isSamplingAudio ? View.VISIBLE : View.GONE);
        } else {
            mBtnMic.setEnabled(false);
            mAudioLevelMeter.setVisibility(View.GONE);
        }

        return disableControls;
    }
}

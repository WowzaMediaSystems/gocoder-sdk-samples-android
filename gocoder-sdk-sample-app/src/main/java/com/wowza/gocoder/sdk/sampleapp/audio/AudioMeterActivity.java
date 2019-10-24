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

package com.wowza.gocoder.sdk.sampleapp.audio;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.errors.WOWZStreamingError;
import com.wowza.gocoder.sdk.support.status.WOWZStatus;
import com.wowza.gocoder.sdk.sampleapp.CameraActivityBase;
import com.wowza.gocoder.sdk.sampleapp.R;
import com.wowza.gocoder.sdk.sampleapp.ui.AudioLevelMeter;
import com.wowza.gocoder.sdk.sampleapp.ui.MultiStateButton;

/**
 * This activity class demonstrates how to create a class that implements the WOWZAudioDevice.AudioSampleListener interface
 * to sample PCM audio from the default audio input device. The {@link AudioLevelMeter} class implements the interface and
 * an instance of that class is registered with the {@code WOWZAudioDevice} API using the registerAudioSampleListener(WOWZAudioDevice.AudioSampleListener)
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

        mBtnMic             = findViewById(R.id.ic_mic);
        mAudioLevelMeter    = findViewById(R.id.audioLevelMeter);
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
     * Click handler for the ToggleBroadcast button
     */
    public void onToggleBroadcast(View v) {
        super.onToggleBroadcast(v, null);
    }

    /**
     * Click handler for the ToggleBroadcast button
     */
    public void onSettings(View v) {
        super.onSettings(v);
    }

    /**
     * Click handler for the mic/mute button
     */
    public void onToggleMute(View v) {
        mBtnMic.toggleState();

        if (getBroadcast().getStatus().isBroadcasting()) {
            mWZAudioDevice.setAudioPaused(!mBtnMic.isOn());
            Toast.makeText(this, "Audio stream " + (mWZAudioDevice.isAudioPaused() ? "muted" : "enabled"), Toast.LENGTH_SHORT).show();
        } else {
            mAudioLevelMeter.setVisibility(mBtnMic.isOn() ? View.VISIBLE : View.GONE);

            if (mBtnMic.isOn()) {
                mWZAudioDevice.startAudioSampler();
                mWZBroadcastConfig.setAudioEnabled(true);
            }else {
                mWZAudioDevice.stopAudioSampler();
                mWZBroadcastConfig.setAudioEnabled(false);
            }
        }
   }
    @Override
    protected synchronized WOWZStreamingError startBroadcast() {
        mRestartAudioSampler = (mWZAudioDevice != null && mWZAudioDevice.isSamplingAudio());
        return super.startBroadcast();
    }

    @Override
    public void onWZStatus(final WOWZStatus goCoderStatus) {
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
            boolean isStreaming = getBroadcast().getStatus().isBroadcasting();
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

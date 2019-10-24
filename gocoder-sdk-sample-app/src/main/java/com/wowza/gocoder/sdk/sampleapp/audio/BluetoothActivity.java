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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.broadcast.WOWZBroadcastConfig;
import com.wowza.gocoder.sdk.api.configuration.WOWZMediaConfig;
import com.wowza.gocoder.sdk.api.errors.WOWZStreamingError;
import com.wowza.gocoder.sdk.api.logging.WOWZLog;
import com.wowza.gocoder.sdk.sampleapp.R;

/**
 * This activity class demonstrates how to use a Bluetooth mic as the audio input device
 * for the GoCoder SDK's WOWZAudioDevice audio device interface and broadcasting class
 */
public class BluetoothActivity  extends AudioMeterActivity {
    private final static String TAG = BluetoothActivity.class.getSimpleName();

    protected final static int BLUETOOTH_CHANNELS       = WOWZMediaConfig.AUDIO_CHANNELS_MONO; // mono
    protected final static int BLUETOOTH_SAMPLE_RATE    = 8000; // 8kHz

    protected ImageView         mBluetoothIcon      = null;

    protected AudioManager      mAudioManager       = null;
    protected BroadcastReceiver mBroadcastReceiver  = null;

    protected WOWZBroadcastConfig mSamplerConfig      = null;

    private int mStoredChannels     = WOWZMediaConfig.AUDIO_CHANNELS_STEREO;
    private int mStoredSampleRate   = WOWZMediaConfig.DEFAULT_AUDIO_SAMPLE_RATE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothIcon = (ImageView) findViewById(R.id.ic_bluetooth);
        mBluetoothIcon.setVisibility(View.VISIBLE);
        mBluetoothIcon.setImageAlpha(128);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                processBluetoothState(state);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    /**
     * Called from {@link AudioMeterActivity#onResume()} to setup the audio input device(s) and audio sampler
     */
    @Override
    protected void setupAudioDevices() {
        if (mWZAudioDevice == null) return;

        // Save the values that are persisted with the shared preferences
        mStoredChannels     = getBroadcastConfig().getAudioChannels();
        mStoredSampleRate   = getBroadcastConfig().getAudioSampleRate();

        if (mSamplerConfig == null) {
            mSamplerConfig = new WOWZBroadcastConfig(getBroadcastConfig());
            mSamplerConfig.setAudioChannels(mStoredChannels);
            mSamplerConfig.setAudioSampleRate(mStoredSampleRate);
        }

        // Start the audio sampler with the default mic selected
        mWZAudioDevice.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mWZAudioDevice.registerAudioSampleListener(mAudioLevelMeter);
        mWZAudioDevice.startAudioSampler(mSamplerConfig);

        mBtnMic.setState(true);
        mBluetoothIcon.setImageAlpha(128);

        // Register the BroadcastReceiver to get notifications of Bluetooth connectivity changes
        Intent intent = registerReceiver(mBroadcastReceiver,
                new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));

        if (intent != null) {
            // Check to see if the Bluetooth device is already connected
            int currentState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            if (currentState == AudioManager.SCO_AUDIO_STATE_CONNECTED)
                // If already connected, the BroadcastReceiver will not receive
                // a state update notification so go ahead and process the state now
                processBluetoothState(currentState);
        }

        // Start Bluetooth SCO audio connection
        WOWZLog.debug(TAG, "Starting Bluetooth SCO audio connection");
        mAudioManager.startBluetoothSco();

        Toast.makeText(this, getString(R.string.audio_bluetooth_help), Toast.LENGTH_LONG).show();
    }

    /**
     * Called from {@link AudioMeterActivity#onPause()} to shutdown/release the audio device(s) and audio sampler
     */
    @Override
    protected void releaseAudioDevices() {
        mBluetoothIcon.setImageAlpha(64);

        if (mWZAudioDevice == null) return;

        if (mWZAudioDevice.isSamplingAudio())
            mWZAudioDevice.stopAudioSampler();

        mWZAudioDevice.unregisterAudioSampleListener(mAudioLevelMeter);
        unregisterReceiver(mBroadcastReceiver);

        // Stop bluetooth SCO audio connection.
        WOWZLog.debug(TAG, "Stopping Bluetooth SCO audio connection");
        mAudioManager.stopBluetoothSco();
    }

    /**
     * Process the Bluetooth connectivity state received via the intent
     * @param state the AudioManager intent
     */
    protected void processBluetoothState(int state) {

        int currentDevice   = mWZAudioDevice.getAudioSource();
        int newDevice       = currentDevice;

        switch(state) {
            case AudioManager.SCO_AUDIO_STATE_CONNECTING:
                WOWZLog.debug(TAG, "ACTION_SCO_AUDIO_STATE_UPDATED: SCO_AUDIO_STATE_CONNECTING");
                break;

            case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                WOWZLog.debug(TAG, "ACTION_SCO_AUDIO_STATE_UPDATED: SCO_AUDIO_STATE_CONNECTED");

                if (!isBluetoothActive()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBluetoothIcon.setImageAlpha(255);
                            Toast.makeText(BluetoothActivity.this, "Bluetooth mic connected", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                // Select the Bluetooth mic now that connectivity has been established
                newDevice = MediaRecorder.AudioSource.MIC;
                break;

            case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                WOWZLog.debug(TAG, "ACTION_SCO_AUDIO_STATE_UPDATED: SCO_AUDIO_STATE_DISCONNECTED");

                if (isBluetoothActive()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBluetoothIcon.setImageAlpha(64);
                            Toast.makeText(BluetoothActivity.this, "Bluetooth mic disconnected", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                // Switch back to the default mic if the Bluetooth mic disconnects
                newDevice = MediaRecorder.AudioSource.CAMCORDER;
                break;

            case AudioManager.SCO_AUDIO_STATE_ERROR:
                WOWZLog.error(TAG, "An error occurred obtaining Bluetooth state information");
                break;
        }

        if (newDevice != currentDevice) {
            // Make sure the audio sampler is shutdown before switching devices
            if (mWZAudioDevice.isSamplingAudio())
                mWZAudioDevice.stopAudioSampler();

            // Select the new device
            mWZAudioDevice.setAudioSource(newDevice);

            // For Bluetooth devices, the channels must be mono and the sampling rate 8kHz
            mSamplerConfig.set(getBroadcastConfig());
            mSamplerConfig.setAudioChannels(isBluetoothActive() ? BLUETOOTH_CHANNELS : mStoredChannels);
            mSamplerConfig.setAudioSampleRate(isBluetoothActive() ? BLUETOOTH_SAMPLE_RATE : mStoredSampleRate);

            // Restart the audio sampler with the appropriate
            // configuration for the newly selected audio input device
            if (mBtnMic.isOn())
                mWZAudioDevice.startAudioSampler(mSamplerConfig);
       }
    }

    /**
     * @return True if the Bluetooth mic is active, False otherwise
     */
    protected boolean isBluetoothActive() {
        return mWZAudioDevice != null && mWZAudioDevice.getAudioSource() == MediaRecorder.AudioSource.MIC;
    }

    /**
     * Ensure broadcast config meets Bluetooth requirements
     * @return Any synchronous error that may have occurred starting the broadcast, null otherwise
     */
    @Override
    protected synchronized WOWZStreamingError startBroadcast() {
        getBroadcastConfig().setAudioChannels(isBluetoothActive() ? BLUETOOTH_CHANNELS : mStoredChannels);
        getBroadcastConfig().setAudioSampleRate(isBluetoothActive() ? BLUETOOTH_SAMPLE_RATE : mStoredSampleRate);

        return super.startBroadcast();
    }

    /**
     * Click handler for the mic/mute button
     */
    @Override
    public void onToggleMute(View v) {
        mBtnMic.toggleState();

        if (getBroadcast().getStatus().isBroadcasting()) {
            mWZAudioDevice.setAudioPaused(!mBtnMic.isOn());
            Toast.makeText(this, "Audio stream " + (mWZAudioDevice.isAudioPaused() ? "muted" : "enabled"), Toast.LENGTH_SHORT).show();
        } else {
            mAudioLevelMeter.setVisibility(mBtnMic.isOn() ? View.VISIBLE : View.GONE);

            if (mBtnMic.isOn()) {
                // Start the audio sampler with the appropriate config for the active audio input device
                mWZAudioDevice.startAudioSampler(mSamplerConfig);
                mWZBroadcastConfig.setAudioEnabled(true);
            }
            else {
                mWZAudioDevice.stopAudioSampler();
                mWZBroadcastConfig.setAudioEnabled(false);
            }
        }
    }
}

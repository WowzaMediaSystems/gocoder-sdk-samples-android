/**
 *
 * This is sample code provided by Wowza Media Systems, LLC.  All sample code is intended to be a reference for the
 * purpose of educating developers, and is not intended to be used in any production environment.
 *
 * IN NO EVENT SHALL WOWZA MEDIA SYSTEMS, LLC BE LIABLE TO YOU OR ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
 * OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION,
 * EVEN IF WOWZA MEDIA SYSTEMS, LLC HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * WOWZA MEDIA SYSTEMS, LLC SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. ALL CODE PROVIDED HEREUNDER IS PROVIDED "AS IS".
 * WOWZA MEDIA SYSTEMS, LLC HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * © 2015 – 2019 Wowza Media Systems, LLC. All rights reserved.
 */

package com.wowza.gocoder.sdk.sampleapp

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView

import com.wowza.gocoder.sdk.api.WowzaGoCoder
import com.wowza.gocoder.sdk.api.configuration.WOWZMediaConfig
import com.wowza.gocoder.sdk.api.data.WOWZDataMap
import com.wowza.gocoder.sdk.api.logging.WOWZLog
import com.wowza.gocoder.sdk.api.player.WOWZPlayerConfig
import com.wowza.gocoder.sdk.api.player.WOWZPlayerView
import com.wowza.gocoder.sdk.api.status.WOWZPlayerStatus
import com.wowza.gocoder.sdk.api.status.WOWZPlayerStatus.PlayerState
import com.wowza.gocoder.sdk.api.status.WOWZPlayerStatusCallback
import com.wowza.gocoder.sdk.sampleapp.config.GoCoderSDKPrefs
import com.wowza.gocoder.sdk.sampleapp.ui.DataTableFragment
import com.wowza.gocoder.sdk.sampleapp.ui.MultiStateButton
import com.wowza.gocoder.sdk.sampleapp.ui.StatusView
import com.wowza.gocoder.sdk.sampleapp.ui.TimerView
import com.wowza.gocoder.sdk.sampleapp.ui.VolumeChangeObserver

class KotlinPlayerActivity : GoCoderSDKActivityBase(), WOWZPlayerStatusCallback {

    // Stream player view
    private var mStreamPlayerView: WOWZPlayerView? = null
    private var mStreamPlayerConfig: WOWZPlayerConfig? = null

    // UI controls
    private var mBtnPlayStream: MultiStateButton? = null
    private var mBtnSettings: MultiStateButton? = null
    private var mBtnMic: MultiStateButton? = null
    private var mBtnScale: MultiStateButton? = null
    private var mSeekVolume: SeekBar? = null
    private var mBufferingDialog: ProgressDialog? = null
    private var mGoingDownDialog: ProgressDialog? = null
    private var mStatusView: StatusView? = null
    private var mHelp: TextView? = null
    private var mTimerView: TimerView? = null
    private var mStreamMetadata: ImageButton? = null
    private var mVolumeSettingChangeObserver: VolumeChangeObserver? = null
    private val callbackHandler = Handler(Looper.getMainLooper())
    private val isNetworkAvailable: Boolean
        get() {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    ?: return false
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream_player)

        mRequiredPermissions = arrayOf()

        mStreamPlayerView = findViewById(R.id.vwStreamPlayer)

        mBtnPlayStream = findViewById(R.id.ic_play_stream)
        mBtnSettings = findViewById(R.id.ic_settings)
        mBtnMic = findViewById(R.id.ic_mic)
        mBtnScale = findViewById(R.id.ic_scale)

        mTimerView = findViewById(R.id.txtTimer)
        mStatusView = findViewById(R.id.statusView)
        mStreamMetadata = findViewById(R.id.imgBtnStreamInfo)
        mHelp = findViewById(R.id.streamPlayerHelp)

        mSeekVolume = findViewById(R.id.sb_volume)

        mTimerView!!.visibility = View.GONE


        if (sGoCoderSDK != null) {

            /*
            Packet change listener setup
             */
            val packetChangeListener = object : WOWZPlayerView.PacketThresholdChangeListener {
                override fun packetsBelowMinimumThreshold(packetCount: Int) {
                    WOWZLog.debug("Packets have fallen below threshold $packetCount... ")

                    //                    activity.runOnUiThread(new Runnable() {
                    //                        public void run() {
                    //                            Toast.makeText(activity, "Packets have fallen below threshold ... ", Toast.LENGTH_SHORT).show();
                    //                        }
                    //                    });
                }

                override fun packetsAboveMinimumThreshold(packetCount: Int) {
                    WOWZLog.debug("Packets have risen above threshold $packetCount ... ")

                    //                    activity.runOnUiThread(new Runnable() {
                    //                        public void run() {
                    //                            Toast.makeText(activity, "Packets have risen above threshold ... ", Toast.LENGTH_SHORT).show();
                    //                        }
                    //                    });
                }
            }
            mStreamPlayerView!!.setShowAllNotificationsWhenBelowThreshold(false)
            mStreamPlayerView!!.setMinimumPacketThreshold(20)
            mStreamPlayerView!!.registerPacketThresholdListener(packetChangeListener)
            ///// End packet change notification listener

            mTimerView!!.setTimerProvider(object : TimerView.TimerProvider {
                override fun getTimecode(): Long {
                    return mStreamPlayerView!!.currentTime
                }

                override fun getDuration(): Long {
                    return mStreamPlayerView!!.duration
                }
            })

            mSeekVolume!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (mStreamPlayerView != null && mStreamPlayerView!!.currentStatus.isPlaying) {
                        mStreamPlayerView!!.volume = progress
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })

            // listen for volume changes from device buttons, etc.
            mVolumeSettingChangeObserver = VolumeChangeObserver(this, Handler())
            applicationContext.contentResolver.registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mVolumeSettingChangeObserver!!)
            mVolumeSettingChangeObserver!!.setVolumeChangeListener { previousLevel, currentLevel ->
                if (mSeekVolume != null)
                    mSeekVolume!!.progress = currentLevel

                if (mStreamPlayerView != null && mStreamPlayerView!!.currentStatus.isPlaying) {
                    mStreamPlayerView!!.volume = currentLevel
                }
            }

            mBtnScale!!.setState(mStreamPlayerView!!.scaleMode == WOWZMediaConfig.FILL_VIEW)

            // The streaming player configuration properties
            mStreamPlayerConfig = WOWZPlayerConfig()

            mBufferingDialog = ProgressDialog(this)
            mBufferingDialog!!.setTitle(R.string.status_buffering)
            mBufferingDialog!!.setMessage(resources.getString(R.string.msg_please_wait))
            mBufferingDialog!!.setButton(DialogInterface.BUTTON_NEGATIVE, resources.getString(R.string.button_cancel)) { dialogInterface, i ->
                /// test

                cancelBuffering()
            }

            mGoingDownDialog = ProgressDialog(this)
            mGoingDownDialog!!.setTitle(R.string.status_buffering)
            mGoingDownDialog!!.setMessage("Please wait while the decoder is shutting down.")

            mStreamPlayerView!!.registerDataEventListener("onClientConnected") { eventName, eventParams ->
                WOWZLog.info(TAG, "onClientConnected data event received:\n" + eventParams.toString(true))

                Handler(Looper.getMainLooper()).post { }

                // this demonstrates how to return a function result back to the original Wowza Streaming Engine
                // function call request
                val functionResult = WOWZDataMap()
                functionResult.put("greeting", "Hello New Client!")

                functionResult
            }
            // testing player data event handler.
            mStreamPlayerView!!.registerDataEventListener("onWowzaData") { eventName, eventParams ->
                var meta = ""
                if (eventParams != null)
                    meta = eventParams.toString()


                WOWZLog.debug("onWZDataEvent -> eventName $eventName = $meta")

                null
            }

            // testing player data event handler.
            mStreamPlayerView!!.registerDataEventListener("onStatus") { eventName, eventParams ->
                if (eventParams != null)
                    WOWZLog.debug("onWZDataEvent -> eventName $eventName = $eventParams")

                null
            }

            // testing player data event handler.
            mStreamPlayerView!!.registerDataEventListener("onTextData") { eventName, eventParams ->
                if (eventParams != null)
                    WOWZLog.debug("onWZDataEvent -> " + eventName + " = " + eventParams.get("text"))

                null
            }
        } else {
            mHelp!!.visibility = View.GONE
            mStatusView!!.setErrorMessage(WowzaGoCoder.getLastError().errorDescription)
        }

    }

    override fun onDestroy() {
        if (mVolumeSettingChangeObserver != null)
            applicationContext.contentResolver.unregisterContentObserver(mVolumeSettingChangeObserver!!)

        super.onDestroy()
    }

    /**
     * Android Activity class methods
     */
    override fun onResume() {
        super.onResume()
        if (mStreamPlayerView!!.currentStatus.state != PlayerState.IDLE && mStreamPlayerView!!.currentStatus.state != PlayerState.PLAYING) {
            showTearingdownDialog()
            mStreamPlayerView!!.stop()

            // Wait for the streaming player to disconnect and shutdown...
            mStreamPlayerView!!.currentStatus.waitForState(PlayerState.IDLE)
            hideTearingdownDialog()
        }
        syncUIControlState()
    }

    /*
    Click handler for network pausing
     */
    fun onPauseNetwork(v: View) {
        val btn = findViewById(R.id.pause_network) as Button
        if (btn.getText().toString().trim({ it <= ' ' }).equals("pause network", ignoreCase = true)) {
            WOWZLog.info("Pausing network...")
            btn.setText(R.string.wz_unpause_network)
            mStreamPlayerView!!.pauseNetworkStack()
        } else {
            WOWZLog.info("Unpausing network... btn.getText(): " + btn.getText())
            btn.setText(R.string.wz_pause_network)
            mStreamPlayerView!!.unpauseNetworkStack()
        }
    }

    fun playStream() {
        if (!this.isNetworkAvailable) {
            displayErrorDialog("No internet connection, please try again later.")
            return
        }
        showBuffering()
        mStreamPlayerView!!.setMaxSecondsWithNoPackets(4)
        mHelp!!.visibility = View.GONE
        val configValidationError = mStreamPlayerConfig!!.validateForPlayback()
        if (configValidationError != null) {
            mStatusView!!.setErrorMessage(configValidationError.errorDescription)
        } else {
            // Set the detail level for network logging output
            mStreamPlayerView!!.logLevel = mWZNetworkLogLevel

            // Set the player's pre-buffer duration as stored in the app prefs
            val preBufferDuration = GoCoderSDKPrefs.getPreBufferDuration(PreferenceManager.getDefaultSharedPreferences(this))

            mStreamPlayerConfig!!.preRollBufferDuration = preBufferDuration
            // Start playback of the live stream
            mStreamPlayerView!!.play(mStreamPlayerConfig, this)
        }
    }

    /**
     * Click handler for the playback button
     */
    fun onTogglePlayStream(v: View) {
        if (mStreamPlayerView!!.currentStatus.isPlaying) {
            mStreamPlayerView!!.stop()
        } else if (mStreamPlayerView!!.isReadyToPlay) {
            this.playStream()
        }
    }


    @Synchronized
    override fun onWZStatus(status: WOWZPlayerStatus) {
        Handler(Looper.getMainLooper()).post {
            when (status.state) {
                PlayerState.BUFFERING -> showBuffering()
                PlayerState.CONNECTING -> showStartingDialog()
                PlayerState.STOPPING -> {
                    hideBuffering()
                    showTearingdownDialog()
                }
                PlayerState.PLAYING -> {
                    hideBuffering()

                    // Keep the screen on while we are playing back the stream
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                    mTimerView!!.startTimer()

                    // Since we have successfully opened up the server connection, store the connection info for auto complete
                    GoCoderSDKPrefs.storeHostConfig(PreferenceManager.getDefaultSharedPreferences(this@KotlinPlayerActivity), mStreamPlayerConfig!!)
                }

                PlayerState.IDLE -> {
                    if (status.lastError != null) {
                        displayErrorDialog(status.lastError)
                    }
                    status.clearLastError()
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                    mTimerView!!.stopTimer()
                    hideTearingdownDialog()
                }
            }
            syncUIControlState()
        }
    }

    @Synchronized
    override fun onWZError(playerStatus: WOWZPlayerStatus) {
        Handler(Looper.getMainLooper()).post {
            displayErrorDialog(playerStatus.lastError)
            syncUIControlState()
        }
    }

    /**
     * Click handler for the mic/mute button
     */
    fun onToggleMute(v: View) {
        mBtnMic!!.toggleState()

        if (mStreamPlayerView != null)
            mStreamPlayerView!!.mute(!mBtnMic!!.isOn)

        mSeekVolume!!.isEnabled = mBtnMic!!.isOn
    }

    fun onToggleScaleMode(v: View) {
        val newScaleMode = if (mStreamPlayerView!!.scaleMode == WOWZMediaConfig.RESIZE_TO_ASPECT) WOWZMediaConfig.FILL_VIEW else WOWZMediaConfig.RESIZE_TO_ASPECT
        mBtnScale!!.setState(newScaleMode == WOWZMediaConfig.FILL_VIEW)
        mStreamPlayerView!!.scaleMode = newScaleMode
    }

    /**
     * Click handler for the metadata button
     */
    fun onStreamMetadata(v: View) {
        val streamMetadata = mStreamPlayerView!!.metadata
        val streamStats = mStreamPlayerView!!.streamStats
        val streamInfo = WOWZDataMap()

        streamInfo.put("- Stream Statistics -", streamStats)
        streamInfo.put("- Stream Metadata -", streamMetadata)
        //streamInfo.put("- Stream Configuration -", streamConfig);

        val dataTableFragment = DataTableFragment.newInstance("Stream Information", streamInfo, false)

        // Display/hide the data table fragment
        fragmentManager.beginTransaction()
                .add(android.R.id.content, dataTableFragment)
                .addToBackStack("metadata_fragment")
                .commit()
    }

    /**
     * Click handler for the settings button
     */
    fun onSettings(v: View) {
        // Display the prefs fragment
        val prefsFragment = GoCoderSDKPrefs.PrefsFragment()
        prefsFragment.setFixedSource(true)
        prefsFragment.setForPlayback(true)

        fragmentManager.beginTransaction()
                .replace(android.R.id.content, prefsFragment)
                .addToBackStack(null)
                .commit()
    }

    /**
     * Update the state of the UI controls
     */
    private fun syncUIControlState() {
        val disableControls = !(mStreamPlayerView!!.isReadyToPlay || mStreamPlayerView!!.currentStatus.isPlaying) // (!(GlobalPlayerStateManager.isReady() ||  mStreamPlayerView.isReadyToPlay() || mStreamPlayerView.isPlaying()) || sGoCoderSDK == null);
        if (disableControls) {
            mBtnPlayStream!!.isEnabled = false
            mBtnSettings!!.isEnabled = false
            mSeekVolume!!.isEnabled = false
            mBtnScale!!.isEnabled = false
            mBtnMic!!.isEnabled = false
            mStreamMetadata!!.isEnabled = false
        } else {
            mBtnPlayStream!!.setState(mStreamPlayerView!!.currentStatus.isPlaying)
            mBtnPlayStream!!.isEnabled = true
            if (mStreamPlayerConfig!!.isAudioEnabled) {
                mBtnMic!!.visibility = View.VISIBLE
                mBtnMic!!.isEnabled = true

                mSeekVolume!!.visibility = View.VISIBLE
                mSeekVolume!!.isEnabled = mBtnMic!!.isOn
                mSeekVolume!!.progress = mStreamPlayerView!!.volume
            } else {
                mSeekVolume!!.visibility = View.GONE
                mBtnMic!!.visibility = View.GONE
            }

            mBtnScale!!.visibility = View.VISIBLE
            mBtnScale!!.visibility = if (mStreamPlayerView!!.currentStatus.isPlaying && mStreamPlayerConfig!!.isVideoEnabled) View.VISIBLE else View.GONE
            mBtnScale!!.isEnabled = mStreamPlayerView!!.currentStatus.isPlaying && mStreamPlayerConfig!!.isVideoEnabled

            mBtnSettings!!.isEnabled = !mStreamPlayerView!!.currentStatus.isPlaying
            mBtnSettings!!.visibility = if (mStreamPlayerView!!.currentStatus.isPlaying) View.GONE else View.VISIBLE

            mStreamMetadata!!.isEnabled = mStreamPlayerView!!.currentStatus.isPlaying
            mStreamMetadata!!.visibility = if (mStreamPlayerView!!.currentStatus.isPlaying) View.VISIBLE else View.GONE
        }
    }

    private fun showStartingDialog() {
        try {
            if (mBufferingDialog == null) return
            //            hideBuffering();
            mBufferingDialog!!.setMessage("Connecting")
            if (!mBufferingDialog!!.isShowing) {
                mBufferingDialog!!.setCancelable(false)
                mBufferingDialog!!.show()
            }
        } catch (ex: Exception) {
            WOWZLog.warn(TAG, "showTearingdownDialog:$ex")
        }

    }

    private fun showTearingdownDialog() {
        try {
            if (mGoingDownDialog == null) return
            hideBuffering()
            if (!mGoingDownDialog!!.isShowing) {
                mGoingDownDialog!!.setCancelable(false)
                mGoingDownDialog!!.show()
            }
        } catch (ex: Exception) {
            WOWZLog.warn(TAG, "showTearingdownDialog:$ex")
        }

    }

    private fun hideTearingdownDialog() {

        try {
            if (mGoingDownDialog == null) return
            hideBuffering()
            mGoingDownDialog!!.dismiss()
        } catch (ex: Exception) {
            WOWZLog.warn(TAG, "hideTearingdownDialog exception:$ex")
        }

    }

    private fun showBuffering() {
        try {
            if (mBufferingDialog == null) return

            if (mBufferingDialog!!.isShowing) {
                mBufferingDialog!!.setMessage(resources.getString(R.string.msg_please_wait))
                return
            }

            val mainThreadHandler = Handler(baseContext.mainLooper)
            mBufferingDialog!!.setCancelable(false)
            mBufferingDialog!!.show()
            mBufferingDialog!!.getButton(DialogInterface.BUTTON_NEGATIVE).isEnabled = false
            object : Thread() {
                override fun run() {

                    mainThreadHandler.post { mBufferingDialog!!.getButton(DialogInterface.BUTTON_NEGATIVE).isEnabled = true }
                }
            }.start()
        } catch (ex: Exception) {
            WOWZLog.warn(TAG, "showBuffering:$ex")
        }

    }

    private fun cancelBuffering() {

        showTearingdownDialog()
        mStreamPlayerView!!.stop()
        hideTearingdownDialog()

    }

    private fun hideBuffering() {
        if (mBufferingDialog != null && mBufferingDialog!!.isShowing)
            mBufferingDialog!!.dismiss()
    }

    override fun syncPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val sLogLevel = prefs.getString("wz_debug_net_log_level", WOWZLog.LOG_LEVEL_DEBUG.toString())
        if (sLogLevel != null)
            mWZNetworkLogLevel = Integer.valueOf(sLogLevel)

        mStreamPlayerConfig!!.isPlayback = true
        if (mStreamPlayerConfig != null)
            GoCoderSDKPrefs.updateConfigFromPrefsForPlayer(prefs, mStreamPlayerConfig!!)
    }

    companion object {
        private val TAG = KotlinPlayerActivity::class.java.simpleName
    }

}

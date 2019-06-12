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
import com.wowza.gocoder.sdk.api.status.WOWZState
import com.wowza.gocoder.sdk.api.status.WOWZStatus
import com.wowza.gocoder.sdk.sampleapp.config.GoCoderSDKPrefs
import com.wowza.gocoder.sdk.sampleapp.ui.DataTableFragment
import com.wowza.gocoder.sdk.sampleapp.ui.MultiStateButton
import com.wowza.gocoder.sdk.sampleapp.ui.StatusView
import com.wowza.gocoder.sdk.sampleapp.ui.TimerView
import com.wowza.gocoder.sdk.sampleapp.ui.VolumeChangeObserver

class KotlinPlayerActivity : GoCoderSDKActivityBase() {

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

    private var mStatusView: StatusView? = null
    private var mHelp: TextView? = null
    private var mTimerView: TimerView? = null
    private var mStreamMetadata: ImageButton? = null

    private var mVolumeSettingChangeObserver: VolumeChangeObserver? = null
    private val isNetworkAvailable: Boolean
        get() {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream_player)

        mRequiredPermissions = arrayOf()

        mStreamPlayerView = findViewById(R.id.vwStreamPlayer) as WOWZPlayerView

        mBtnPlayStream = findViewById(R.id.ic_play_stream) as MultiStateButton
        mBtnSettings = findViewById(R.id.ic_settings) as MultiStateButton
        mBtnMic = findViewById(R.id.ic_mic) as MultiStateButton
        mBtnScale = findViewById(R.id.ic_scale) as MultiStateButton

        mTimerView = findViewById(R.id.txtTimer) as TimerView
        mStatusView = findViewById(R.id.statusView) as StatusView
        mStreamMetadata = findViewById(R.id.imgBtnStreamInfo) as ImageButton
        mHelp = findViewById(R.id.streamPlayerHelp) as TextView

        mSeekVolume = findViewById(R.id.sb_volume) as SeekBar

        mTimerView!!.visibility = View.GONE


        if (GoCoderSDKActivityBase.sGoCoderSDK != null) {
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
                    if (mStreamPlayerView != null && mStreamPlayerView!!.isPlaying) {
                        mStreamPlayerView!!.volume = progress
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })

            // listen for volume changes from device buttons, etc.
            mVolumeSettingChangeObserver = VolumeChangeObserver(this, Handler())
            applicationContext.contentResolver.registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mVolumeSettingChangeObserver!!)
            mVolumeSettingChangeObserver!!.setVolumeChangeListener { _, currentLevel ->
                if (mSeekVolume != null)
                    mSeekVolume!!.progress = currentLevel

                if (mStreamPlayerView != null && mStreamPlayerView!!.isPlaying) {
                    mStreamPlayerView!!.volume = currentLevel
                }
            }

            mBtnScale!!.setState(mStreamPlayerView!!.scaleMode == WOWZMediaConfig.FILL_VIEW)

            // The streaming player configuration properties
            mStreamPlayerConfig = WOWZPlayerConfig()

            mBufferingDialog = ProgressDialog(this)
            mBufferingDialog!!.setTitle(R.string.status_buffering)
            mBufferingDialog!!.setMessage(resources.getString(R.string.msg_please_wait))
            mBufferingDialog!!.setButton(DialogInterface.BUTTON_NEGATIVE, resources.getString(R.string.button_cancel)) { dialogInterface, i -> cancelBuffering() }

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

        syncUIControlState()
    }

    override fun onPause() {
        if (mStreamPlayerView != null && mStreamPlayerView!!.isPlaying) {
            mStreamPlayerView!!.stop()

            // Wait for the streaming player to disconnect and shutdown...
            mStreamPlayerView!!.currentStatus.waitForState(WOWZState.IDLE)
        }

        super.onPause()
    }

    /**
     * Click handler for network pausing
     */
    fun onPauseNetwork(v: View) {
        val btn = findViewById<Button>(R.id.pause_network)
        val btnTitle = btn.text.toString().trim()
        if (btnTitle.equals("pause network")) {
            WOWZLog.info("Pausing network...")
            btn.text = getResources().getString(R.string.wz_unpause_network)
            mStreamPlayerView!!.pauseNetworkStack()
        } else {
            WOWZLog.info("Unpausing network... btn.getText(): " + btn.text)
            btn.text = getResources().getString(R.string.wz_pause_network)
            mStreamPlayerView!!.unpauseNetworkStack()
        }
    }

    /**
     * Click handler for the playback button
     */
    fun onTogglePlayStream(v: View) {
        if (mStreamPlayerView!!.isPlaying) {
            mStreamPlayerView!!.stop()
        } else if (mStreamPlayerView!!.isReadyToPlay) {
            if (!this.isNetworkAvailable) {
                displayErrorDialog("No internet connection, please try again later.")
                return
            }

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
    }

    /**
     * WOWZStatusCallback interface methods
     */
    @Synchronized
    override fun onWZStatus(status: WOWZStatus) {
        val playerStatus = WOWZStatus(status)

        Handler(Looper.getMainLooper()).post {
            WOWZStatus(playerStatus.state)
            when (playerStatus.state) {

                WOWZPlayerView.STATE_PLAYING -> {
                    // Keep the screen on while we are playing back the stream
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                    if (mStreamPlayerConfig!!.preRollBufferDuration == 0f) {
                        mTimerView!!.startTimer()
                    }

                    // Since we have successfully opened up the server connection, store the connection info for auto complete

                    GoCoderSDKPrefs.storeHostConfig(PreferenceManager.getDefaultSharedPreferences(this@KotlinPlayerActivity), mStreamPlayerConfig!!)

                    // Log the stream metadata
                    WOWZLog.debug(TAG, "Stream metadata:\n" + mStreamPlayerView!!.metadata)
                }

                WOWZPlayerView.STATE_READY_TO_PLAY -> {
                    // Clear the "keep screen on" flag
                    WOWZLog.debug(TAG, "STATE_READY_TO_PLAY player activity status!")
                    if (playerStatus.lastError != null)
                        displayErrorDialog(playerStatus.lastError)

                    playerStatus.clearLastError()
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                    mTimerView!!.stopTimer()
                }

                WOWZPlayerView.STATE_PREBUFFERING_STARTED -> {
                    WOWZLog.debug(TAG, "Dialog for buffering should show...")
                    showBuffering()
                }

                WOWZPlayerView.STATE_PREBUFFERING_ENDED -> {
                    WOWZLog.debug(TAG, "Dialog for buffering should stop...")
                    hideBuffering()
                    // Make sure player wasn't signaled to shutdown
                    if (mStreamPlayerView!!.isPlaying) {
                        mTimerView!!.startTimer()
                    }
                }

                else -> {
                }
            }
            syncUIControlState()
        }
    }

    @Synchronized
    override fun onWZError(playerStatus: WOWZStatus) {
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
        //        WOWZDataMap streamConfig = mStreamPlayerView.getStreamConfig().toDataMap();
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
        val disableControls = !(mStreamPlayerView!!.isReadyToPlay || mStreamPlayerView!!.isPlaying) || GoCoderSDKActivityBase.sGoCoderSDK == null
        if (disableControls) {
            mBtnPlayStream!!.isEnabled = false
            mBtnSettings!!.isEnabled = false
            mSeekVolume!!.isEnabled = false
            mBtnScale!!.isEnabled = false
            mBtnMic!!.isEnabled = false
            mStreamMetadata!!.isEnabled = false
        } else {
            mBtnPlayStream!!.setState(mStreamPlayerView!!.isPlaying)
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
            mBtnScale!!.visibility = if (mStreamPlayerView!!.isPlaying && mStreamPlayerConfig!!.isVideoEnabled) View.VISIBLE else View.GONE
            mBtnScale!!.isEnabled = mStreamPlayerView!!.isPlaying && mStreamPlayerConfig!!.isVideoEnabled

            mBtnSettings!!.isEnabled = !mStreamPlayerView!!.isPlaying
            mBtnSettings!!.visibility = if (mStreamPlayerView!!.isPlaying) View.GONE else View.VISIBLE

            mStreamMetadata!!.isEnabled = mStreamPlayerView!!.isPlaying
            mStreamMetadata!!.visibility = if (mStreamPlayerView!!.isPlaying) View.VISIBLE else View.GONE
        }
    }

    private fun showBuffering() {
        try {
            if (mBufferingDialog == null) return
            mBufferingDialog!!.show()
        } catch (ex: Exception) {
            WOWZLog.warn("showBuffering exception: $ex")
        }

    }

    private fun cancelBuffering() {
        if (mStreamPlayerConfig!!.hlsBackupURL != null || mStreamPlayerConfig!!.isHLSEnabled) {
            mStreamPlayerView!!.stop()
        } else if (mStreamPlayerView != null && mStreamPlayerView!!.isPlaying) {
            mStreamPlayerView!!.stop()
        }
    }

    private fun hideBuffering() {
        if (mBufferingDialog!!.isShowing)
            mBufferingDialog!!.dismiss()
    }

    override fun syncPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        mWZNetworkLogLevel = Integer.valueOf(prefs.getString("wz_debug_net_log_level", WOWZLog.LOG_LEVEL_DEBUG.toString()))

        mStreamPlayerConfig!!.setIsPlayback(true)
        if (mStreamPlayerConfig != null)
            GoCoderSDKPrefs.updateConfigFromPrefsForPlayer(prefs, mStreamPlayerConfig!!)
    }

    companion object {
        private val TAG = PlayerActivity::class.java.getSimpleName()
    }

}

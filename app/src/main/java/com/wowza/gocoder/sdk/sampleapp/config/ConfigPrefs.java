/**
 *  This code and all components (c) Copyright 2015-2016, Wowza Media Systems, LLC. All rights reserved.
 *  This code is licensed pursuant to the BSD 3-Clause License.
 */
package com.wowza.gocoder.sdk.sampleapp.config;

import android.content.SharedPreferences;

import com.wowza.gocoder.sdk.api.configuration.WZMediaConfig;
import com.wowza.gocoder.sdk.api.configuration.WZStreamConfig;
import com.wowza.gocoder.sdk.api.configuration.WowzaConfig;
import com.wowza.gocoder.sdk.api.devices.WZCamera;
import com.wowza.gocoder.sdk.api.h264.WZProfileLevel;

/**
 * Updates the GoCoder SDK broadcast configuration with the stored preference values
 */
public class ConfigPrefs {

    public final static int ALL_PREFS               = 0x1;
    public final static int CONNECTION_ONLY_PREFS   = 0x2;
    public final static int VIDEO_AND_CONNECTION    = 0x3;

    public final static String PREFS_TYPE           = "header_resource";
    public final static String VIDEO_CONFIGS        = "configs";
    public final static String H264_PROFILE_LEVELS  = "profile_levels";
    public final static String FIXED_FRAME_SIZE     = "fixed_frame_size";
    public final static String FIXED_FRAME_RATE     = "fixed_frame_rate";

    /**
     * Update the video and audio preferences
     */
    public static void updateConfigFromPrefs(SharedPreferences sharedPrefs, WZMediaConfig mediaConfig) {
        // video settings
        mediaConfig.setVideoEnabled(sharedPrefs.getBoolean("wz_video_enabled", true));

        mediaConfig.setVideoFrameWidth(sharedPrefs.getInt("wz_video_frame_width", WZMediaConfig.DEFAULT_VIDEO_FRAME_WIDTH));
        mediaConfig.setVideoFrameHeight(sharedPrefs.getInt("wz_video_frame_height", WZMediaConfig.DEFAULT_VIDEO_FRAME_HEIGHT));
        mediaConfig.setVideoFramerate(Integer.parseInt(sharedPrefs.getString("wz_video_frame_rate", String.valueOf(WZMediaConfig.DEFAULT_VIDEO_FRAME_RATE))));
        mediaConfig.setVideoKeyFrameInterval(Integer.parseInt(sharedPrefs.getString("wz_video_keyframe_interval", String.valueOf(WZMediaConfig.DEFAULT_VIDEO_KEYFRAME_INTERVAL))));
        mediaConfig.setVideoBitRate(Integer.parseInt(sharedPrefs.getString("wz_video_bitrate", String.valueOf(WZMediaConfig.DEFAULT_VIDEO_BITRATE))));

        int profile = sharedPrefs.getInt("wz_video_profile_level_profile", -1);
        int level = sharedPrefs.getInt("wz_video_profile_level_level", -1);
        if (profile != -1 && level != -1) {
            WZProfileLevel profileLevel = new WZProfileLevel(profile, level);
            if (profileLevel.validate()) {
                mediaConfig.setVideoProfileLevel(profileLevel);
            }
        } else {
            mediaConfig.setVideoProfileLevel(null);
        }

        // audio settings
        mediaConfig.setAudioEnabled(sharedPrefs.getBoolean("wz_audio_enabled", true));

        mediaConfig.setAudioSampleRate(Integer.parseInt(sharedPrefs.getString("wz_audio_sample_rate", String.valueOf(WZMediaConfig.DEFAULT_AUDIO_SAMPLE_RATE))));
        mediaConfig.setAudioChannels(sharedPrefs.getBoolean("wz_audio_stereo", true) ? WZMediaConfig.AUDIO_CHANNELS_STEREO : WZMediaConfig.AUDIO_CHANNELS_MONO);
        mediaConfig.setAudioBitRate(Integer.parseInt(sharedPrefs.getString("wz_audio_bitrate", String.valueOf(WZMediaConfig.DEFAULT_AUDIO_BITRATE))));
    }

    /**
     * Update the connection preferences
     */
    public static void updateConfigFromPrefs(SharedPreferences sharedPrefs, WZStreamConfig streamConfig) {
        // connection settings
        streamConfig.setHostAddress(sharedPrefs.getString("wz_live_host_address", null));
        streamConfig.setPortNumber(Integer.parseInt(sharedPrefs.getString("wz_live_port_number", String.valueOf(WowzaConfig.DEFAULT_PORT))));
        streamConfig.setApplicationName(sharedPrefs.getString("wz_live_app_name", WowzaConfig.DEFAULT_APP));
        streamConfig.setStreamName(sharedPrefs.getString("wz_live_stream_name", WowzaConfig.DEFAULT_STREAM));
        streamConfig.setUsername(sharedPrefs.getString("wz_live_username", null));
        streamConfig.setPassword(sharedPrefs.getString("wz_live_password", null));

        updateConfigFromPrefs(sharedPrefs, (WZMediaConfig) streamConfig);
    }

    /**
     * Update the platform specific preferences
     */
    public static void updateConfigFromPrefs(SharedPreferences sharedPrefs, WowzaConfig wowzaConfig) {
        // WowzaConfig-specific properties
        wowzaConfig.setCapturedVideoRotates(sharedPrefs.getBoolean("wz_captured_video_rotates", true));

        updateConfigFromPrefs(sharedPrefs, (WZStreamConfig) wowzaConfig);
    }

    /**
     * Store the active camera id as a preference
     */
    public static void setActiveCamera(SharedPreferences sharedPrefs, int cameraId) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt("wz_camera_id", cameraId);
        editor.apply();
    }

    /**
     * Retrieve the active camera id preference value
     */
    public static int getActiveCamera(SharedPreferences sharedPrefs) {
        return sharedPrefs.getInt("wz_camera_id", WZCamera.DIRECTION_BACK);
    }
}

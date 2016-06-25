package com.wowza.gocoder.sdk.sampleapp.config;
/**
 *  ConfigPrefs.java
 *  gocoder-sdk-sampleapp
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

import android.content.SharedPreferences;

import com.wowza.gocoder.sdk.api.configuration.WZMediaConfig;
import com.wowza.gocoder.sdk.api.configuration.WZStreamConfig;
import com.wowza.gocoder.sdk.api.configuration.WowzaConfig;
import com.wowza.gocoder.sdk.api.devices.WZCamera;
import com.wowza.gocoder.sdk.api.h264.WZProfileLevel;

public class ConfigPrefs {

    public final static int ALL_PREFS               = 0x1;
    public final static int CONNECTION_ONLY_PREFS   = 0x2;
    public final static int VIDEO_AND_CONNECTION    = 0x3;

    public final static String PREFS_TYPE           = "header_resource";
    public final static String VIDEO_CONFIGS        = "configs";
    public final static String H264_PROFILE_LEVELS  = "profile_levels";
    public final static String FIXED_FRAME_SIZE     = "fixed_frame_size";
    public final static String FIXED_FRAME_RATE     = "fixed_frame_rate";

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

        mediaConfig.setAudioSampleRate(Integer.parseInt(sharedPrefs.getString("wz_audio_samplerate", String.valueOf(WZMediaConfig.DEFAULT_AUDIO_SAMPLE_RATE))));
        mediaConfig.setAudioChannels(sharedPrefs.getBoolean("wz_audio_stereo", true) ? WZMediaConfig.AUDIO_CHANNELS_STEREO : WZMediaConfig.AUDIO_CHANNELS_MONO);
        mediaConfig.setAudioBitRate(Integer.parseInt(sharedPrefs.getString("wz_audio_bitrate", String.valueOf(WZMediaConfig.DEFAULT_AUDIO_BITRATE))));
    }

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

    public static void updateConfigFromPrefs(SharedPreferences sharedPrefs, WowzaConfig wowzaConfig) {
        // WowzaConfig-specific properties
        wowzaConfig.setCapturedVideoRotates(sharedPrefs.getBoolean("wz_captured_video_rotates", true));

        updateConfigFromPrefs(sharedPrefs, (WZStreamConfig) wowzaConfig);
    }

    public static void setActiveCamera(SharedPreferences sharedPrefs, int cameraId) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt("wz_camera_id", cameraId);
        editor.apply();
    }

    public static int getActiveCamera(SharedPreferences sharedPrefs) {
        return sharedPrefs.getInt("wz_camera_id", WZCamera.DIRECTION_BACK);
    }

    public static int getScaleMode(SharedPreferences sharedPrefs) {
        return sharedPrefs.getBoolean("wz_video_resize_to_aspect", false) ? WZMediaConfig.RESIZE_TO_ASPECT : WZMediaConfig.FILL_VIEW;
    }
}

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
import android.text.TextUtils;

import com.wowza.gocoder.sdk.api.broadcast.WZBroadcastConfig;
import com.wowza.gocoder.sdk.api.configuration.WZMediaConfig;
import com.wowza.gocoder.sdk.api.configuration.WZStreamConfig;
import com.wowza.gocoder.sdk.api.configuration.WowzaConfig;
import com.wowza.gocoder.sdk.api.devices.WZCamera;
import com.wowza.gocoder.sdk.api.h264.WZProfileLevel;
import com.wowza.gocoder.sdk.api.logging.WZLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class ConfigPrefs {
    private final static String TAG = ConfigPrefs.class.getSimpleName();

    private final static String AUTO_COMPLETE_SUFFIX = "_auto_complete";
    private final static String AUTO_COMPLETE_HOST_CONFIG_PREFIX = "wz_live_host_config_";

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
        mediaConfig.setABREnabled(sharedPrefs.getBoolean("wz_video_use_abr", true));

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

    public static int getScaleMode(SharedPreferences sharedPrefs) {
        return sharedPrefs.getBoolean("wz_video_resize_to_aspect", false) ? WZMediaConfig.RESIZE_TO_ASPECT : WZMediaConfig.FILL_VIEW;
    }

    public static void storeAutoCompleteHostConfig(SharedPreferences sharedPrefs, WZBroadcastConfig broadcastConfig) {
        String hostAddress = broadcastConfig.getHostAddress();
        if (hostAddress == null || hostAddress.trim().length() == 0) return;

        updateAutoCompleteHostsList(sharedPrefs, hostAddress);

        updateAutoCompleteList(sharedPrefs, "wz_live_port_number", Integer.toString(broadcastConfig.getPortNumber()));
        updateAutoCompleteList(sharedPrefs, "wz_live_app_name", broadcastConfig.getApplicationName());
        updateAutoCompleteList(sharedPrefs, "wz_live_stream_name", broadcastConfig.getStreamName());
        updateAutoCompleteList(sharedPrefs, "wz_live_username", broadcastConfig.getUsername());

        String hostConfigKey = hostConfigKey(hostAddress);
        String storedConfig = (
                    broadcastConfig.getPortNumber())
                    + ";" +
                    (broadcastConfig.getApplicationName() != null ? broadcastConfig.getApplicationName().trim() : "")
                    + ";" +
                    (broadcastConfig.getStreamName() != null ? broadcastConfig.getStreamName().trim() : "")
                    + ";" +
                    (broadcastConfig.getUsername() != null ? broadcastConfig.getUsername().trim() : ""
                );

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(hostConfigKey, storedConfig);
        editor.apply();

        //logAutoCompleteLists(sharedPrefs);
    }

    public static WZStreamConfig loadAutoCompleteHostConfig(SharedPreferences sharedPrefs, String hostAddress) {
        if (hostAddress == null || hostAddress.trim().length() == 0) return null;

        String hostConfigKey = hostConfigKey(hostAddress);
        if (!sharedPrefs.contains(hostConfigKey)) return null;

        String storedConfig = sharedPrefs.getString(hostConfigKey, null);
        if (storedConfig == null) return null;

        String storedValues[] = TextUtils.split(storedConfig, ";");
        if (storedValues.length != 4) {
            removeAutoCompleteHostConfig(sharedPrefs, hostAddress);
            return null;
        }

        try {
            int port_number = Integer.parseInt(storedValues[0]);

            WZStreamConfig hostConfig = new WZStreamConfig();
            hostConfig.setHostAddress(hostAddress);
            hostConfig.setPortNumber(port_number);
            if (storedValues[1].trim().length()>0) hostConfig.setApplicationName(storedValues[1]);
            if (storedValues[2].trim().length()>0) hostConfig.setStreamName(storedValues[2]);
            if (storedValues[3].trim().length()>0) hostConfig.setUsername(storedValues[3]);

            return hostConfig;
        } catch (NumberFormatException e) {
            removeAutoCompleteHostConfig(sharedPrefs, hostAddress);
            return null;
        }
    }

    private static void removeAutoCompleteHostConfig(SharedPreferences sharedPrefs, String hostAddress) {
        if (hostAddress == null || hostAddress.trim().length() == 0) return;

        String hostConfigKey = hostConfigKey(hostAddress);
        if (!sharedPrefs.contains(hostConfigKey)) return;

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.remove(hostConfigKey);
        editor.apply();
    }

    private static void updateAutoCompleteList(SharedPreferences sharedPrefs, String prefKey, String newValue) {
        if (prefKey == null || prefKey.trim().length() == 0) return;
        if (newValue == null || newValue.trim().length() == 0) return;

        Set<String> currentSet = sharedPrefs.getStringSet(autoCompleteKey(prefKey), null);
        TreeSet<String> curValues = currentSet != null ? new TreeSet<String>(currentSet) : new TreeSet<String>();

        for(String str: curValues) {
            if(str.equalsIgnoreCase(newValue.trim()))
                return;
        }
        curValues.add(newValue.trim());

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putStringSet(autoCompleteKey(prefKey), curValues);
        editor.apply();
    }

    private static boolean updateAutoCompleteHostsList(SharedPreferences sharedPrefs, String hostAddress) {
        if (hostAddress == null || hostAddress.trim().length() == 0) return false;

        Set<String> currentSet = sharedPrefs.getStringSet(autoCompleteKey("wz_live_host_address"), null);
        ArrayList<String> currentList = (currentSet != null ?  new ArrayList<String>(currentSet) : new ArrayList<String>());

        String currentEntry = null;
        for(String storedHost: currentList) {
            if(storedHost.equalsIgnoreCase(hostAddress.trim()))
                currentEntry = storedHost;
        }
        if (currentEntry != null) currentSet.remove(currentEntry);

        currentList.add(0, hostAddress);
        currentSet = new HashSet<String>(currentList);

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putStringSet(autoCompleteKey("wz_live_host_address"), currentSet);
        editor.apply();

        return true;
    }

    public static String[] getAutoCompleteList(SharedPreferences sharedPrefs, String prefKey) {
        if (!sharedPrefs.contains(autoCompleteKey(prefKey))) return new String[0];

        Set<String> currentSet = sharedPrefs.getStringSet(autoCompleteKey(prefKey), null);
        if (currentSet == null) return new String[0];

        return currentSet.toArray(new String[currentSet.size()]);
    }

    public static void clearAutoCompleteList(SharedPreferences sharedPrefs, String prefKey) {
        if (prefKey == null || prefKey.trim().length() == 0) return;
        if (!sharedPrefs.contains(autoCompleteKey(prefKey))) return;

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.remove(autoCompleteKey(prefKey));
        editor.apply();
    }

    public static void clearAllAutoCompleteLists(SharedPreferences sharedPrefs) {

        clearAutoCompleteList(sharedPrefs, "wz_live_port_number");
        clearAutoCompleteList(sharedPrefs, "wz_live_app_name");
        clearAutoCompleteList(sharedPrefs, "wz_live_stream_name");
        clearAutoCompleteList(sharedPrefs, "wz_live_username");

        String[] hosts = getAutoCompleteList(sharedPrefs, "wz_live_host_address");
        clearAutoCompleteList(sharedPrefs, "wz_live_host_address");

        SharedPreferences.Editor editor = sharedPrefs.edit();
        for(String hostAddress: hosts) {
            String hostConfigKey = hostConfigKey(hostAddress);
            if (sharedPrefs.contains(hostConfigKey))
                editor.remove(hostConfigKey);
        }
        editor.apply();
    }

    private static String autoCompleteKey(String prefKey) {
        if (prefKey == null || prefKey.trim().length() == 0) return null;
        return prefKey + AUTO_COMPLETE_SUFFIX;
    }

    private static String hostConfigKey(String hostAddress) {
        if (hostAddress == null || hostAddress.trim().length() == 0) return null;
        return AUTO_COMPLETE_HOST_CONFIG_PREFIX + hostAddress.trim().toLowerCase();
    }

    public static void logAutoCompleteLists(SharedPreferences sharedPrefs) {
        String[] hosts = getAutoCompleteList(sharedPrefs, "wz_live_host_address");

        StringBuilder logData = new StringBuilder(
                            "wz_live_host_address = " + Arrays.toString(hosts) + "\n" +
                            "wz_live_port_number  = " + Arrays.toString(getAutoCompleteList(sharedPrefs, "wz_live_port_number")) + "\n" +
                            "wz_live_app_name     = " + Arrays.toString(getAutoCompleteList(sharedPrefs, "wz_live_app_name")) + "\n" +
                            "wz_live_stream_name  = " + Arrays.toString(getAutoCompleteList(sharedPrefs, "wz_live_stream_name")) + "\n" +
                            "wz_live_username     = " + Arrays.toString(getAutoCompleteList(sharedPrefs, "wz_live_username")) + "\n");

        for(String hostAddress: hosts) {
            logData.append("\nhostConfig for " + hostAddress + ":\n\n");
            WZStreamConfig hostConfig = loadAutoCompleteHostConfig(sharedPrefs, hostAddress);
            logData.append(hostConfig.toString() + "\n");
        }

        WZLog.debug(TAG, logData.toString());
    }
}

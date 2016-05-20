/**
 *  This code and all components (c) Copyright 2015-2016, Wowza Media Systems, LLC. All rights reserved.
 *  This code is licensed pursuant to the BSD 3-Clause License.
 */
package com.wowza.gocoder.sdk.sampleapp.config;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.wowza.gocoder.sdk.api.configuration.WZMediaConfig;
import com.wowza.gocoder.sdk.api.geometry.WZSize;
import com.wowza.gocoder.sdk.api.h264.WZProfileLevel;
import com.wowza.gocoder.sdk.sampleapp.R;

import java.util.Arrays;
import java.util.List;

/**
 * Display and persist a set of application preferences
 */
public class ConfigPrefsActivity extends PreferenceActivity {

    private static WZMediaConfig[] sVideoConfigs = null;
    private static WZProfileLevel[] sProfileLevels = null;
    protected static boolean sFixedFrameSize = false;
    protected static boolean sFixedFrameRate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        Intent intent = getIntent();

        // workaround for ClassCastException bug in older Android versions
        // http://stackoverflow.com/questions/28720062/class-cast-exception-when-passing-array-of-serializables-from-one-activity-to-an
        Object[] objectArray = (Object[]) intent.getSerializableExtra(ConfigPrefs.VIDEO_CONFIGS);
        if (objectArray != null) {
            sVideoConfigs = Arrays.copyOf(objectArray, objectArray.length, WZMediaConfig[].class);
        }

        Object[] objectArray2 = (Object[]) intent.getSerializableExtra(ConfigPrefs.H264_PROFILE_LEVELS);
        if (objectArray2 != null) {
            sProfileLevels = Arrays.copyOf(objectArray2, objectArray2.length, WZProfileLevel[].class);
        }

        sFixedFrameSize = intent.getBooleanExtra(ConfigPrefs.FIXED_FRAME_SIZE, false);
        sFixedFrameRate = intent.getBooleanExtra(ConfigPrefs.FIXED_FRAME_RATE, false);

        int header_resource = getIntent().getIntExtra(ConfigPrefs.PREFS_TYPE, -1);
        if (header_resource != -1) {
            switch (header_resource) {
                case ConfigPrefs.ALL_PREFS:
                    loadHeadersFromResource(R.xml.capture_pref_headers, target);
                    break;
                case ConfigPrefs.CONNECTION_ONLY_PREFS:
                    loadHeadersFromResource(R.xml.connection_only_pref_headers, target);
                    break;
                case ConfigPrefs.VIDEO_AND_CONNECTION:
                    loadHeadersFromResource(R.xml.video_and_connection, target);
                    break;
            }
        }
    }

    public static class ConnectionSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.connection_preferences);
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

   public static class VideoSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.video_preferences);

            ListPreference videoSizes = (ListPreference) findPreference("wz_video_size");
            ListPreference profileLevels = (ListPreference) findPreference("wz_video_profile_level");
            final EditTextPreference bitRate = (EditTextPreference)findPreference("wz_video_bitrate");

            if (sVideoConfigs == null || sVideoConfigs.length == 0 || sFixedFrameSize) {
                getPreferenceScreen().removePreference(videoSizes);
            }
            else {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                int prefWidth = sharedPreferences.getInt("wz_video_frame_width", WZMediaConfig.DEFAULT_VIDEO_FRAME_WIDTH);
                int prefHeight = sharedPreferences.getInt("wz_video_frame_height", WZMediaConfig.DEFAULT_VIDEO_FRAME_HEIGHT);
                WZSize prefSize = new WZSize(prefWidth, prefHeight);
                int prefIndex = sVideoConfigs.length - 1;

                String[] entries = new String[sVideoConfigs.length];
                String[] entryValues = new String[sVideoConfigs.length];
                for(int i=0; i < sVideoConfigs.length; i++) {
                    entries[i] = sVideoConfigs[i].getLabel(true, true, true, true);
                    entryValues[i] = String.valueOf(i);
                    if (sVideoConfigs[i].getVideoFrameSize().equals(prefSize))
                        prefIndex = i;
                }

                videoSizes.setEntries(entries);
                videoSizes.setEntryValues(entryValues);
                videoSizes.setValueIndex(prefIndex);

                videoSizes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o instanceof String) {
                        try {
                            int selectedIndex = Integer.parseInt((String) o);

                            if (selectedIndex >= 0 && selectedIndex < sVideoConfigs.length) {
                                WZMediaConfig selectedConfig = sVideoConfigs[selectedIndex];

                                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
                                prefsEditor.putInt("wz_video_frame_width", selectedConfig.getVideoFrameWidth());
                                prefsEditor.putInt("wz_video_frame_height", selectedConfig.getVideoFrameHeight());
                                prefsEditor.putString("wz_video_bitrate", String.valueOf(selectedConfig.getVideoBitRate()));
                                prefsEditor.apply();

                                bitRate.setText(String.valueOf(selectedConfig.getVideoBitRate()));

                                return true;
                            }
                        } catch (NumberFormatException e) {
                            // bad no. returned
                        }
                    }
                    return false;
                    }
                });
            }

            if (sProfileLevels == null || sProfileLevels.length == 0) {
                getPreferenceScreen().removePreference(profileLevels);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
                prefsEditor.putInt("wz_video_profile_level_profile", -1);
                prefsEditor.putInt("wz_video_profile_level_level", -1);
                prefsEditor.apply();
            }
            else {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                int profile = sharedPreferences.getInt("wz_video_profile_level_profile", -1);
                int level = sharedPreferences.getInt("wz_video_profile_level_level", -1);
                WZProfileLevel profileLevel = new WZProfileLevel(profile, level);
                int prefIndex = sProfileLevels.length;

                String[] entries = new String[sProfileLevels.length+1];
                String[] entryValues = new String[sProfileLevels.length+1];
                for(int i=0; i < sProfileLevels.length; i++) {
                    entries[i] = sProfileLevels[i].toString();
                    entryValues[i] = String.valueOf(i);
                    if (sProfileLevels[i].equals(profileLevel))
                        prefIndex = i;
                }
                entries[sProfileLevels.length] = "(none)";
                entryValues[sProfileLevels.length] = "-1";

                profileLevels.setEntries(entries);
                profileLevels.setEntryValues(entryValues);
                profileLevels.setValueIndex(prefIndex);

                profileLevels.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        if (o instanceof String) {
                            try {
                                int selectedIndex = Integer.parseInt((String) o);

                                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                SharedPreferences.Editor prefsEditor = sharedPreferences.edit();

                                if (selectedIndex >= 0 && selectedIndex < sProfileLevels.length) {
                                    WZProfileLevel selectedProfileLevel = sProfileLevels[selectedIndex];
                                    prefsEditor.putInt("wz_video_profile_level_profile", selectedProfileLevel.getProfile());
                                    prefsEditor.putInt("wz_video_profile_level_level", selectedProfileLevel.getLevel());
                                } else {
                                    prefsEditor.putInt("wz_video_profile_level_profile", -1);
                                    prefsEditor.putInt("wz_video_profile_level_level", -1);
                                }

                                prefsEditor.apply();
                                return true;

                            } catch (NumberFormatException e) {
                                // bad no. returned
                            }
                        }
                        return false;
                    }
                });
            }

            if (sFixedFrameRate) {
                EditTextPreference frameRate = (EditTextPreference)findPreference("wz_video_framerate");
                getPreferenceScreen().removePreference(frameRate);
            }
        }
    }

    public static class AudioSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.audio_preferences);
        }
    }

}

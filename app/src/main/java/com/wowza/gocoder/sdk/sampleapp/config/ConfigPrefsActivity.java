/**
 *  ConfigPrefsActivity.java
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
import com.wowza.gocoder.sdk.api.configuration.WZStreamConfig;
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
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.connection_preferences);

            String[] prefIds = {
                    //"wz_live_host_address",
                    "wz_live_port_number",
                    "wz_live_app_name",
                    "wz_live_stream_name",
                    "wz_live_username"
            };

            String[] passwordPrefIds = {
                    "wz_live_password"
            };

            configurePrefTitles(this, prefIds, passwordPrefIds);

            final PreferenceFragment fragment = this;

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Preference hostAddress = findPreference("wz_live_host_address");

            final int prefTitleRes = hostAddress.getTitleRes();
            prefSetTitle(sharedPreferences, hostAddress, "wz_live_host_address", getResources().getString(hostAddress.getTitleRes()));

            hostAddress.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o instanceof String) {

                        // Update host address title
                        prefSetTitle(preference, fragment.getResources().getString(prefTitleRes), (String)o);

                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        WZStreamConfig hostConfig = ConfigPrefs.loadAutoCompleteHostConfig(sharedPreferences, (String)o);

                        if (hostConfig != null) {

                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("wz_live_port_number", String.valueOf(hostConfig.getPortNumber()));
                            editor.putString("wz_live_app_name", hostConfig.getApplicationName());
                            editor.putString("wz_live_stream_name", hostConfig.getStreamName());
                            editor.putString("wz_live_username", hostConfig.getUsername());
                            editor.apply();

                            AutoCompletePreference pref = (AutoCompletePreference)findPreference("wz_live_port_number");
                            prefSetTextAndTitle(pref,
                                    getResources().getString(R.string.wz_live_port_number_title),
                                    String.valueOf(hostConfig.getPortNumber()));

                            pref = (AutoCompletePreference)findPreference("wz_live_app_name");
                            prefSetTextAndTitle(pref,
                                    getResources().getString(R.string.wz_live_app_name_title),
                                    hostConfig.getApplicationName());

                            pref = (AutoCompletePreference)findPreference("wz_live_stream_name");
                            prefSetTextAndTitle(pref,
                                    getResources().getString(R.string.wz_live_stream_name_title),
                                    hostConfig.getStreamName());

                            pref = (AutoCompletePreference)findPreference("wz_live_username");
                            prefSetTextAndTitle(pref,
                                    getResources().getString(R.string.wz_live_username_title),
                                    hostConfig.getUsername());
                        }
                        return true;
                    }
                    return false;
                }
            });


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

            final PreferenceFragment fragment = this;
            final int sizesTitleRes = videoSizes.getTitleRes();
            final int bitRateTitleRes = bitRate.getTitleRes();
            final int profileTitleRes = profileLevels.getTitleRes();

            String[] prefIds = {
                    "wz_video_framerate",
                    "wz_video_keyframe_interval",
                    "wz_video_bitrate"
            };

            configurePrefTitles(this, prefIds);

            if (sVideoConfigs == null || sVideoConfigs.length == 0 || sFixedFrameSize) {
                getPreferenceScreen().removePreference(videoSizes);
            }
            else {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                int prefWidth = sharedPreferences.getInt("wz_video_frame_width", WZMediaConfig.DEFAULT_VIDEO_FRAME_WIDTH);
                int prefHeight = sharedPreferences.getInt("wz_video_frame_height", WZMediaConfig.DEFAULT_VIDEO_FRAME_HEIGHT);

                WZSize prefSize = new WZSize(prefWidth, prefHeight);
                prefSetTitle(videoSizes, getResources().getString(videoSizes.getTitleRes()), prefSize.toString());

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

                                    WZSize prefSize = new WZSize(selectedConfig.getVideoFrameWidth(), selectedConfig.getVideoFrameHeight());
                                    prefSetTitle(preference, fragment.getResources().getString(sizesTitleRes), prefSize.toString());

                                    bitRate.setText(String.valueOf(selectedConfig.getVideoBitRate()));
                                    prefSetTitle(bitRate, fragment.getResources().getString(bitRateTitleRes), String.valueOf(selectedConfig.getVideoBitRate()));

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

                prefSetTitle(profileLevels, getResources().getString(profileTitleRes), (prefIndex != sProfileLevels.length ? profileLevel.toString() : null));

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
                                    prefSetTitle(preference, fragment.getResources().getString(profileTitleRes), selectedProfileLevel.toString());
                                } else {
                                    prefsEditor.putInt("wz_video_profile_level_profile", -1);
                                    prefsEditor.putInt("wz_video_profile_level_level", -1);
                                    prefSetTitle(preference, fragment.getResources().getString(profileTitleRes), null);
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

            String[] prefIds = {
                    "wz_audio_samplerate",
                    "wz_audio_bitrate"
            };

            configurePrefTitles(this, prefIds);
        }
    }

    private static void configurePrefTitles(final PreferenceFragment fragment, final String[] textPrefIds, final String[] passwordPrefIds) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(fragment.getActivity());

        for(String prefId : textPrefIds) {
            Preference pref = fragment.findPreference(prefId);
            final int prefTitleRes = pref.getTitleRes();
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(fragment.getActivity());
                    SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
                    prefsEditor.putString(preference.getKey(), (String)o);
                    prefsEditor.apply();

                    prefSetTitle(preference, fragment.getResources().getString(prefTitleRes), (String)o);
                    return false;
                }
            });
            prefSetTitle(sharedPreferences, pref, prefId, fragment.getResources().getString(pref.getTitleRes()));
        }

        for(String passwordPrefId : passwordPrefIds) {
            Preference pref = fragment.findPreference(passwordPrefId);
            final int prefTitleRes = pref.getTitleRes();
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(fragment.getActivity());
                    SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
                    prefsEditor.putString(preference.getKey(), (String)o);
                    prefsEditor.apply();

                    prefSetTitle(preference, fragment.getResources().getString(prefTitleRes), (String)o, true);
                    return false;
                }
            });
            prefSetTitle(sharedPreferences, pref, passwordPrefId, fragment.getResources().getString(pref.getTitleRes()), true);
        }
    }

    private static void configurePrefTitles(final PreferenceFragment fragment, final String[] textPrefIds) {
        configurePrefTitles(fragment, textPrefIds, new String[0]);
    }

    private static void prefSetTitle(Preference pref, String baseTitle, String prefValue, boolean passwordField) {
        String prefTitle;

        if (prefValue == null || prefValue.trim().length() == 0)
            prefTitle = baseTitle + " (not set)";
        else if (passwordField) {
            char[] masked = new char[prefValue.length()];
            Arrays.fill(masked, '*');
            prefTitle = baseTitle + " [" + String.valueOf(masked) + "]";
        } else
            prefTitle = baseTitle + " [" + prefValue + "]";

        pref.setTitle(prefTitle);
    }

    private static void prefSetTitle(Preference pref, String baseTitle, String prefValue) {
        prefSetTitle(pref, baseTitle, prefValue,false);
    }

    private static void prefSetTextAndTitle(EditTextPreference pref, String baseTitle, String prefValue) {
        pref.setText(prefValue);
        prefSetTitle(pref, baseTitle, prefValue,false);
    }

    private static void prefSetTitle(SharedPreferences sharedPreferences, Preference pref, String prefName, String baseTitle, boolean passwordField) {
        String prefValue = sharedPreferences.getString(prefName, null);

        prefSetTitle(pref, baseTitle, prefValue, passwordField);
    }

    private static void prefSetTitle(SharedPreferences sharedPreferences, Preference pref, String prefName, String baseTitle) {
        prefSetTitle(sharedPreferences, pref, prefName, baseTitle, false);
    }

}

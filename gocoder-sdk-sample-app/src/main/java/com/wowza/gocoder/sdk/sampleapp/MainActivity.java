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

package com.wowza.gocoder.sdk.sampleapp;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.wowza.gocoder.sdk.api.WOWZVersionInfo;
import com.wowza.gocoder.sdk.sampleapp.ui.AboutFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends ListActivity {

    private static final String TITLE = "example_title";
    private static final String DESCRIPTION = "example_description";
    private static final String ICON = "example_icon";
    private static final String CLASS_NAME = "class_name";

    private static final String[][] ACTIVITY_TEXT = {
            {   "Stream live video and audio",
                    "Broadcast a live video and audio stream captured with the local camera and mic",
                    "CameraActivity"                        },
            {   "Play a live stream",
                    "Displays an active live stream from Wowza Streaming Engine",
                    "PlayerActivity"            },

            {   "Kotlin Player",
                    "Displays an active live stream from Wowza Streaming Engine as written in Kotlin",
                    "KotlinPlayerActivity"            },

            {   "Capture an MP4 file",
                    "Broadcast a live video stream while saving it to an MP4 file",
                    "mp4.MP4CaptureActivity"            },

            {   "Stream an MP4 file",
                    "Broadcast video from an MP4 file stored on the local device",
                    "mp4.MP4BroadcastActivity"          },


            {   "Capture OpenGL ES output",
                    "Stream the output from an OpenGL ES renderer",
                    "graphics.OpenGLActivity"      },

            {   "Display a bitmap overlay",
                    "Display a bitmap as an overlay on the camera preview",
                    "graphics.BitmapOverlayActivity"    },

            {   "Display a text overlay",
                    "Display text as an overlay on the camera preview",
                    "graphics.TextOverlayActivity"      },

            {   "Grab a screenshot",
                    "Take a snapshot of the camera preview",
                    "ScreenshotActivity"      },

            {   "Display an audio level meter",
                    "Register an audio sample listener and display an audio level meter",
                    "audio.AudioMeterActivity"          },

            {   "Use a Bluetooth mic for audio capture",
                    "Use a Bluetooth mic for streaming audio if present",
                    "audio.BluetoothActivity"          },

            {   "Event and Metadata APIs",
                    "Send and receive bi-directional data events while streaming",
                    "EventActivity"                        },

            {   "Display detailed SDK and device information",
                    "Demonstrates the informational APIs available in the SDK",
                    "AboutFragment"                      }
    };

    private static final int[] ACTIVITY_ICONS = {
            R.drawable.ic_streaming,
            R.drawable.ic_player,
            R.drawable.ic_player, 
            R.drawable.ic_mp4_capture,
            R.drawable.ic_mp4_streaming,
            R.drawable.ic_opengl,
            R.drawable.ic_bitmap,
            R.drawable.ic_text_overlay,
            R.drawable.ic_take_screenshot,
            R.drawable.ic_audio_meter,
            R.drawable.ic_bluetooth,
            R.drawable.ic_event,
            R.drawable.ic_info
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getActionBar() != null) {
            getActionBar().setTitle(getResources().getString(R.string.app_name_long));
        }

        setListAdapter(new SimpleAdapter(this, createActivityList(),
                R.layout.example_row,
                new String[] { TITLE, DESCRIPTION, ICON },
                new int[] { R.id.example_title, R.id.example_description, R.id.example_icon } ));

        WOWZVersionInfo sdkVersionInfo = WOWZVersionInfo.getInstance();

        String sdkPreRelease = sdkVersionInfo.getPreRelease();
        String preReleaseText =  (sdkPreRelease != null && sdkPreRelease.trim().length() > 0 ?
                (sdkPreRelease.substring(0,1).toLowerCase().equals("a") ? " (ALPHA)" : " (BETA)") :
                "");

        String sdkVersionText = sdkVersionInfo.toString() + " build no. " + sdkVersionInfo.getBuildNumber() + preReleaseText;

        TextView txtVersion = findViewById(R.id.txtSDKVersion);
        txtVersion.setText( String.format( getResources().getString(R.string.gocoder_version), sdkVersionText) );
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        Map<String, Object> map = (Map<String, Object>)listView.getItemAtPosition(position);
        if (map.get(CLASS_NAME) instanceof String && map.get(CLASS_NAME).equals("AboutFragment")) {
            AboutFragment aboutFragment = AboutFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, aboutFragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            if(map.get(CLASS_NAME) instanceof String && isKotlinMissingWarning(map.get(CLASS_NAME).toString())){
                showKotlinWarning(map.get(CLASS_NAME).toString());
            }
            else {
                Intent intent = (Intent) map.get(CLASS_NAME);
                startActivity(intent);
            }
        }
    }

    // Return correctly from any fragments launched and placed on the back stack
    @Override
    public void onBackPressed(){
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private boolean isKotlinMissingWarning(String className){
        return className.contains("Kotlin");
    }

    private void showKotlinWarning(String className){
        if(isKotlinMissingWarning(className)){
            new AlertDialog.Builder(this)
                    .setTitle("Class Not Found!")
                    .setMessage("KotlinPlayerActivity requires the Kotlin plugin for Android Studio.  Please ensure this is installed and try again.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton(android.R.string.yes, null).show();
        }
    }


    private List<Map<String, Object>> createActivityList() {
        List<Map<String, Object>> activityList = new ArrayList<Map<String, Object>>();

        for (int i=0;i<ACTIVITY_TEXT.length; i++) {
            String activity_text[] = ACTIVITY_TEXT[i];
            int activity_icon = ACTIVITY_ICONS[i];

            Map<String, Object> tmp = new HashMap<String, Object>();
            tmp.put(TITLE, activity_text[0]);
            tmp.put(DESCRIPTION, activity_text[1]);
            tmp.put(ICON, activity_icon);

            if (activity_text[2].contains("Activity")) {
                Intent intent = new Intent();
                try {
                    Class cls = Class.forName("com.wowza.gocoder.sdk.sampleapp." + activity_text[2]);
                    intent.setClass(this, cls);
                    tmp.put(CLASS_NAME, intent);
                } catch (ClassNotFoundException cnfe) {
                    if(!isKotlinMissingWarning(activity_text[2])){
                        throw new RuntimeException("Unable to find " + activity_text[2], cnfe);
                    }
                    tmp.put(CLASS_NAME, activity_text[2]);
                }
            } else {
                tmp.put(CLASS_NAME, activity_text[2]);
            }

            activityList.add(tmp);
        }

        return activityList;
    }
}

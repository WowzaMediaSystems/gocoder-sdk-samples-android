package com.wowza.gocoder.sdk.sampleapp;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

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

            {   "Capture an MP4 file",
                    "Broadcast a live video stream while saving it to an MP4 file",
                    "mp4.MP4CaptureActivity"            },

            {   "Stream an MP4 file",
                    "Broadcast video from an MP4 file stored on the local device",
                    "mp4.MP4BroadcastActivity"          },

            {   "Grab a screenshot",
                    "Take a snapshot of the camera preview",
                    "ScreenshotActivity"      },

            {   "Display a bitmap overlay",
                    "Display a bitmap as an overlay on the camera preview",
                    "graphics.BitmapOverlayActivity"    },

            {   "Display a text overlay",
                    "Display text as an overlay on the camera preview",
                    "graphics.TextOverlayActivity"      },

            {   "Facial recognition demo",
                    "Use facial recognition features to become a Wowza Ninja",
                    "FaceActivity"                      },

            {   "Display an audio level meter",
                    "Register an audio sample listener and display an audio level meter",
                    "audio.AudioMeterActivity"          },

            {   "Use a Bluetooth mic for audio capture",
                    "Use a Bluetooth mic for streaming audio if present",
                    "audio.BluetoothActivity"          },

            {   "Display detailed device information",
                    "Demonstrates the informational APIs available in the SDK",
                    "InfoActivity"                      }
    };

    private static final int[] ACTIVITY_ICONS = {
            R.drawable.ic_streaming,
            R.drawable.ic_mp4_capture,
            R.drawable.ic_mp4_streaming,
            R.drawable.ic_take_screenshot,
            R.drawable.ic_bitmap,
            R.drawable.ic_text_overlay,
            R.drawable.ic_face,
            R.drawable.ic_audio_meter,
            R.drawable.ic_bluetooth,
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
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        Map<String, Object> map = (Map<String, Object>)listView.getItemAtPosition(position);
        Intent intent = (Intent) map.get(CLASS_NAME);
        startActivity(intent);
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

            Intent intent = new Intent();
            try {
                Class cls = Class.forName("com.wowza.gocoder.sdk.sampleapp." + activity_text[2]);
                intent.setClass(this, cls);
                tmp.put(CLASS_NAME, intent);
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException("Unable to find " + activity_text[2], cnfe);
            }

            activityList.add(tmp);
        }

        return activityList;
    }
}

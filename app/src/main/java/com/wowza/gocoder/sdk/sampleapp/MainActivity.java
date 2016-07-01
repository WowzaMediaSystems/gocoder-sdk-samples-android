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
    private static final String CLASS_NAME = "class_name";

    private static final String[][] ACTIVITIES = {
            {   "Stream live video and audio",
                "Broadcast a live video and audio stream captured with the local camera and mic",
                "CameraActivity"                        },

            {   "Capture an MP4 file",
                    "Broadcast a live video stream while saving it to an MP4 file",
                    "mp4.MP4CaptureActivity"            },

            {   "Display a bitmap overlay",
                    "Display a bitmap as an overlay on the camera preview",
                    "graphics.BitmapOverlayActivity"    },

            {   "Stream an MP4 file",
                    "Broadcast video from an MP4 file stored on the local device",
                    "mp4.MP4BroadcastActivity"          },

            {   "Display an audio level meter",
                    "Register an audio sample listener and display an audio level meter",
                    "audio.AudioMeterActivity"          }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().setTitle(getResources().getString(R.string.app_name_long));

        setListAdapter(new SimpleAdapter(this, createActivityList(),
                R.layout.example_row, new String[] { TITLE, DESCRIPTION },
                new int[] { R.id.example_title, R.id.example_description } ));
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        Map<String, Object> map = (Map<String, Object>)listView.getItemAtPosition(position);
        Intent intent = (Intent) map.get(CLASS_NAME);
        startActivity(intent);
    }

    private List<Map<String, Object>> createActivityList() {
        List<Map<String, Object>> activityList = new ArrayList<Map<String, Object>>();

        for (String[] activity : ACTIVITIES) {
            Map<String, Object> tmp = new HashMap<String, Object>();
            tmp.put(TITLE, activity[0]);
            tmp.put(DESCRIPTION, activity[1]);

            Intent intent = new Intent();
            try {
                Class cls = Class.forName("com.wowza.gocoder.sdk.sampleapp." + activity[2]);
                intent.setClass(this, cls);
                tmp.put(CLASS_NAME, intent);
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException("Unable to find " + activity[2], cnfe);
            }

            activityList.add(tmp);
        }

        return activityList;
    }
}

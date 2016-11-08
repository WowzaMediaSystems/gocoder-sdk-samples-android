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
 *  Copyright © 2015 Wowza Media Systems, LLC. All rights reserved.
 */
package com.wowza.gocoder.sdk.sampleapp;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.WZPlatformInfo;
import com.wowza.gocoder.sdk.api.WZVersionInfo;
import com.wowza.gocoder.sdk.api.WowzaGoCoder;
import com.wowza.gocoder.sdk.api.devices.WZCamera;
import com.wowza.gocoder.sdk.api.logging.WZLog;

import java.util.ArrayList;
import java.util.List;

public class InfoActivity extends Activity {

    private final static String TAG = InfoActivity.class.getSimpleName();

    private static final String SDK_SAMPLE_APP_LICENSE_KEY = "GOSK-5442-0101-750D-4A14-FB5C";
    private static final int PERMISSIONS_REQUEST_CODE = 0x1;

    protected String[] mRequiredPermissions = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };
    protected boolean mPermissionsGranted = false;

    // GoCoder SDK top level interface
    protected static WowzaGoCoder sGoCoderSDK = null;

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    List<String> listDataChild;
    Menu actionMenu;
    ShareActionProvider shareActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.info_list);

        if (sGoCoderSDK == null) {
            // Enable detailed logging from the GoCoder SDK
            WZLog.LOGGING_ENABLED = true;

            // Initialize the GoCoder SDK
            sGoCoderSDK = WowzaGoCoder.init(this, SDK_SAMPLE_APP_LICENSE_KEY);

            if (sGoCoderSDK == null) {
                WZLog.error(TAG, WowzaGoCoder.getLastError());
            }
        }
    }

    /**
     * Android Activity lifecycle methods
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (sGoCoderSDK != null) {
            mPermissionsGranted = true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mPermissionsGranted = (mRequiredPermissions.length > 0 ? WowzaGoCoder.hasPermissions(this, mRequiredPermissions) : true);
                if (!mPermissionsGranted)
                    ActivityCompat.requestPermissions(this, mRequiredPermissions, PERMISSIONS_REQUEST_CODE);
            }

            if (mPermissionsGranted) {
                prepareListData();
                listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);
                expListView.setAdapter(listAdapter);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                String uriText =
                        "mailto:support@wowza.com" +
                                "?subject=" + Uri.encode("GoCoder SDK Support Information") +
                                "&body=" + Uri.encode(shareContents());

                Uri uri = Uri.parse(uriText);

                Intent sendIntent = new Intent(Intent.ACTION_SENDTO);
                sendIntent.setData(uri);
                startActivity(Intent.createChooser(sendIntent, "Send to Wowza Support"));

                return true;

            case R.id.action_copy:
                ClipboardManager myClipboard;
                myClipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);

                ClipData myClip;
                myClip = ClipData.newPlainText("text", shareContents());
                myClipboard.setPrimaryClip(myClip);

                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        mPermissionsGranted = true;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                for(int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        mPermissionsGranted = false;
                    }
                }
            }
        }
    }

    private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new ArrayList<String>();

        listDataHeader.add("GoCoder SDK Version Information");
        listDataChild.add(WZVersionInfo.getInstance().toVerboseString());

        listDataHeader.add("Device Model and Platform Information");
        listDataChild.add(WowzaGoCoder.PLATFORM_INFO );

        listDataHeader.add("Display Device Information");
        listDataChild.add(WZPlatformInfo.displayInfo(this));

        listDataHeader.add("Camera Information");
        listDataChild.add(WZCamera.getCameraInfo());

        listDataHeader.add("Video and Audio Encoder Information");
        listDataChild.add(WZPlatformInfo.getEncoderInfo());

        listDataHeader.add("OpenGL ES Information");
        listDataChild.add(WZPlatformInfo.getEglInfo());
    }

    private String shareContents() {
        return  WZVersionInfo.getInstance().toVerboseString() + "\n\n" +
                "GoCoder SDK Version Information:\n" + WowzaGoCoder.PLATFORM_INFO + "\n\n" +
                "Device Model and Platform Information:\n" + WZPlatformInfo.displayInfo(this) + "\n\n" +
                "Display Device Information:\n" + WZCamera.getCameraInfo() + "\n\n" +
                "Video and Audio Encoder Information:\n" + WZPlatformInfo.getEncoderInfo() + "\n\n" +
                "OpenGL ES Information:\n" + WZPlatformInfo.getEglInfo();
    }

    public static class ExpandableListAdapter extends BaseExpandableListAdapter {

        private Context _context;
        private List<String> _listDataHeader;
        private List<String> _listDataChild;

        public ExpandableListAdapter(Context context, List<String> listDataHeader,
                                                        List<String> listChildData) {
            this._context = context;
            this._listDataHeader = listDataHeader;
            this._listDataChild = listChildData;
        }

        @Override
        public Object getChild(int groupPosition, int childPosititon) {
            return this._listDataChild.get(groupPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {

            final String childText = (String) getChild(groupPosition, childPosition);

            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.list_info_item, null);
            }

            TextView txtListChild = (TextView) convertView.findViewById(R.id.info_list_item);

            txtListChild.setText(childText);
            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return 1;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return this._listDataHeader.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return this._listDataHeader.size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            String headerTitle = (String) getGroup(groupPosition);
            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) this._context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.list_info_group, null);
            }

            TextView lblListHeader = (TextView) convertView
                    .findViewById(R.id.info_list_header);
            lblListHeader.setTypeface(null, Typeface.BOLD);
            lblListHeader.setText(headerTitle);

            ExpandableListView expListView = (ExpandableListView) parent;
            expListView.expandGroup(groupPosition);
            expListView.setGroupIndicator(null);

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}

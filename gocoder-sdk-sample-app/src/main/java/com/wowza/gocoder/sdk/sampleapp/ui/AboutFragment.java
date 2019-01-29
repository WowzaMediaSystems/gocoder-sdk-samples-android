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

package com.wowza.gocoder.sdk.sampleapp.ui;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import com.wowza.gocoder.sdk.api.WOWZVersionInfo;
import com.wowza.gocoder.sdk.api.WOWZPlatformInfo;
import com.wowza.gocoder.sdk.api.WowzaGoCoder;
import com.wowza.gocoder.sdk.api.android.opengl.WOWZGLES;
import com.wowza.gocoder.sdk.api.codec.WOWZCodecUtils;
import com.wowza.gocoder.sdk.api.devices.WOWZCamera;
import com.wowza.gocoder.sdk.api.logging.WOWZLog;
import com.wowza.gocoder.sdk.sampleapp.R;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.CLIPBOARD_SERVICE;

public class AboutFragment extends Fragment {

    //private OnFragmentInteractionListener mListener;

    private ExpandableListAdapter  mListAdapter;
    private ExpandableListView     mListView;

    public AboutFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AboutFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mListAdapter = prepareListAdapter();
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_copy:
                ClipboardManager myClipboard;
                myClipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);

                ClipData myClip;
                myClip = ClipData.newPlainText("text", shareContents());
                myClipboard.setPrimaryClip(myClip);

                Toast.makeText(getActivity(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_about, container, false);
        if (fragmentView != null)
            fragmentView.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.background_dark));

        // Populate the list view
        ExpandableListView listView = (ExpandableListView) fragmentView.findViewById(R.id.info_list);
        listView.setAdapter(mListAdapter);

        WOWZVersionInfo sdkVersionInfo = WOWZVersionInfo.getInstance();
        String versionText = "v" + sdkVersionInfo.toString() + " build no. " + String.valueOf(sdkVersionInfo.getBuildNumber());
        TextView versionNumber = (TextView) fragmentView.findViewById(R.id.txtProductVersion);
        versionNumber.setText(versionText);

        return fragmentView;
    }

/*
    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }
*/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //mListener = null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
/*
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
/*
     public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
     }
*/
    private ExpandableListAdapter prepareListAdapter() {
        List<String> listDataHeader;
        List<String> listDataChild;

        listDataHeader = new ArrayList<String>();
        listDataChild = new ArrayList<String>();

/*
        listDataHeader.add(getResources().getString(R.string.about_product_label));
        listDataChild.add(WOWZVersionInfo.getInstance().toVerboseString());
*/

        listDataHeader.add(getResources().getString(R.string.about_header_platform));
        listDataChild.add(WowzaGoCoder.PLATFORM_INFO);

        listDataHeader.add(getResources().getString(R.string.about_header_display));
        listDataChild.add(WOWZPlatformInfo.displayInfo(getActivity()));

        listDataHeader.add(getResources().getString(R.string.about_header_camera));
        listDataChild.add(getMyCameraInfo());

        listDataHeader.add(getResources().getString(R.string.about_header_codecs));
        listDataChild.add(WOWZCodecUtils.getCodecInfo());

        listDataHeader.add(getResources().getString(R.string.about_header_opengles));
        listDataChild.add(WOWZGLES.getEglInfo(false));

        return new ExpandableListAdapter(getActivity(), listDataHeader, listDataChild);
    }

    private String getMyCameraInfo(){
        try{
            String[] mRequiredPermissions =new String[] {
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            };
            boolean result = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                result = (mRequiredPermissions.length > 0 ? WowzaGoCoder.hasPermissions(this.getContext(), mRequiredPermissions) : true);
                if (!result) {
                    ActivityCompat.requestPermissions(this.getActivity(), mRequiredPermissions, 0x1);
                }
            }

            if(result) {
                if (WOWZCamera.getAvailableDeviceCameras(this.getActivity().getApplicationContext()).length > 0) {
                    return WOWZCamera.getCameraInfo(this.getActivity().getApplicationContext());
                }
            }
        }
        catch(Exception ex){
            WOWZLog.error(ex.getMessage());
        }
        return "";
    }

    private String shareContents() {

        return  (getResources().getString(R.string.about_product_label) + ":\n" + WOWZVersionInfo.getInstance().toVerboseString() + "\n\n" +
                getResources().getString(R.string.about_header_platform) + ":\n" + WowzaGoCoder.PLATFORM_INFO + "\n\n" +
                getResources().getString(R.string.about_header_display) + ":\n" + WOWZPlatformInfo.displayInfo(getActivity()) + "\n\n" +
                getResources().getString(R.string.about_header_camera) + ":\n" + getMyCameraInfo() + "\n\n" +
                getResources().getString(R.string.about_header_codecs) + ":\n" + WOWZCodecUtils.getCodecInfo() + "\n\n" +
                getResources().getString(R.string.about_header_opengles) + ":\n" + WOWZGLES.getEglInfo(false))
                .replaceAll("\t+", " ")
                .replaceAll(" +", " ");
    }

    public static class ExpandableListAdapter extends BaseExpandableListAdapter {

        private Context         _context;
        private List<String>    _listDataHeader;
        private List<String>    _listDataChild;

        public ExpandableListAdapter(Context context, List<String> listDataHeader,
                                     List<String> listChildData) {
            this._context = context;
            this._listDataHeader = listDataHeader;
            this._listDataChild = listChildData;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
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
                LayoutInflater infalInflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.list_info_group, null);
            }

            TextView lblListHeader = (TextView) convertView.findViewById(R.id.info_list_header);
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

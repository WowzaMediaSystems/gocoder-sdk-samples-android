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

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.devices.WOWZCamera;
import com.wowza.gocoder.sdk.api.devices.WOWZCameraView;
import com.wowza.gocoder.sdk.api.geometry.WOWZSize;

public class AutoFocusListener extends GestureDetector.SimpleOnGestureListener {

    private Context         mContext    = null;
    private WOWZCameraView mCameraView = null;

    public AutoFocusListener(Context context) {
        super();
        mContext = context;
    }

    public AutoFocusListener(Context context, WOWZCameraView cameraView) {
        this(context);
        mCameraView = cameraView;
    }

    public void setCameraView(WOWZCameraView mCameraView) {
        this.mCameraView = mCameraView;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public boolean onDown(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        if (mCameraView != null) {
            WOWZCamera activeCamera = mCameraView.getCamera();

            if (activeCamera != null && activeCamera.hasCapability(WOWZCamera.FOCUS_MODE_CONTINUOUS)) {
                if (activeCamera.getFocusMode() != WOWZCamera.FOCUS_MODE_CONTINUOUS) {
                    activeCamera.setFocusMode(WOWZCamera.FOCUS_MODE_CONTINUOUS);
                    Toast.makeText(mContext, "Continuous video focus on", Toast.LENGTH_SHORT).show();
                } else {
                    activeCamera.setFocusMode(WOWZCamera.FOCUS_MODE_OFF);
                    Toast.makeText(mContext, "Continuous video focus off", Toast.LENGTH_SHORT).show();
                }
            }
        }

        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        if (mCameraView != null) {
            WOWZCamera activeCamera = mCameraView.getCamera();

            if (activeCamera != null && activeCamera.hasCapability(WOWZCamera.FOCUS_MODE_AUTO)) {

                DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
                WOWZSize previewScreenSize = mCameraView.getScreenSize();
                WOWZSize previewFrameSize = mCameraView.getFrameSize();

                int previewScreenLeft = Math.round((float) (displayMetrics.widthPixels - previewScreenSize.width) / 2f);
                int previewScreenTop = Math.round((float) (displayMetrics.heightPixels - previewScreenSize.height) / 2f);

                float previewScreenX = event.getX() - previewScreenLeft;
                float previewScreenY = event.getY() - previewScreenTop;

                if (previewScreenX < 0 || previewScreenX > previewScreenSize.width ||
                        previewScreenY < 0 || previewScreenY > previewScreenSize.getHeight())
                    return true;

                float relX = (previewScreenX / (float)previewScreenSize.width) * (float)previewFrameSize.getWidth();
                float relY = (previewScreenY / (float)previewScreenSize.height) * (float)previewFrameSize.getHeight();

                Toast.makeText(mContext, "Auto focusing at (" + Math.round(relX) + "," + Math.round(relY) + ")", Toast.LENGTH_SHORT).show();
                activeCamera.setFocusPoint(relX, relY, 25);
            }
        }

        return true;
    }
}

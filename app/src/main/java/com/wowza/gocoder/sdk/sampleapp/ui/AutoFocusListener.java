package com.wowza.gocoder.sdk.sampleapp.ui;/*
 *
 * WOWZA MEDIA SYSTEMS, LLC ("Wowza") CONFIDENTIAL
 * Copyright (c) 2005-2016 Wowza Media Systems, LLC, All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property of Wowza Media Systems, LLC.
 * The intellectual and technical concepts contained herein are proprietary to Wowza Media Systems, LLC
 * and may be covered by U.S. and Foreign Patents, patents in process, and are protected by trade secret
 * or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from Wowza Media Systems, LLC. Access to the source code
 * contained herein is hereby forbidden to anyone except current Wowza Media Systems, LLC employees, managers
 * or contractors who have executed Confidentiality and Non-disclosure agreements explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication or disclosure of this
 * source code, which includes information that is confidential and/or proprietary, and is a trade secret, of
 * Wowza Media Systems, LLC. ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF WOWZA MEDIA SYSTEMS, LLC IS
 * STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES. THE RECEIPT OR POSSESSION
 * OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR
 * DISTRIBUTE ITS CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 *
 */

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.devices.WZCamera;
import com.wowza.gocoder.sdk.api.devices.WZCameraView;
import com.wowza.gocoder.sdk.api.geometry.WZSize;

public class AutoFocusListener extends GestureDetector.SimpleOnGestureListener {

    private Context         mContext    = null;
    private WZCameraView    mCameraView = null;

    public AutoFocusListener(Context context) {
        super();
        mContext = context;
    }

    public AutoFocusListener(Context context, WZCameraView cameraView) {
        this(context);
        mCameraView = cameraView;
    }

    public void setCameraView(WZCameraView mCameraView) {
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
            WZCamera activeCamera = mCameraView.getCamera();

            if (activeCamera != null && activeCamera.hasCapability(WZCamera.FOCUS_MODE_CONTINUOUS)) {
                if (activeCamera.getFocusMode() != WZCamera.FOCUS_MODE_CONTINUOUS) {
                    activeCamera.setFocusMode(WZCamera.FOCUS_MODE_CONTINUOUS);
                    Toast.makeText(mContext, "Continuous video focus on", Toast.LENGTH_SHORT).show();
                } else {
                    activeCamera.setFocusMode(WZCamera.FOCUS_MODE_OFF);
                    Toast.makeText(mContext, "Continuous video focus off", Toast.LENGTH_SHORT).show();
                }
            }
        }

        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        if (mCameraView != null) {
            WZCamera activeCamera = mCameraView.getCamera();

            if (activeCamera != null && activeCamera.hasCapability(WZCamera.FOCUS_MODE_AUTO)) {

                DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
                WZSize previewScreenSize = mCameraView.getScreenSize();
                WZSize previewFrameSize = mCameraView.getFrameSize();

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

/*
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

package com.wowza.gocoder.sdk.sampleapp.graphics;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.android.graphics.WZBitmap;
import com.wowza.gocoder.sdk.sampleapp.CameraActivityBase;
import com.wowza.gocoder.sdk.sampleapp.R;

/**
 * This activity class demonstrates use of the WZBitmap API to display a bitmap
 * as an overlay within the GoCoder SDK camera preview display
 */
public class BitmapOverlayActivity extends CameraActivityBase {
    private final static String TAG = BitmapOverlayActivity.class.getSimpleName();

    private WZBitmap mWZBitmap = null;
    private ScaleGestureDetector mScaleDetector = null;
    private float mScaleFactor = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overlay);

        mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sGoCoderSDK != null && mWZCameraView != null && mWZBitmap == null) {
            // Read in a PNG file from the app resources as a bitmap
            Bitmap overlayBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.overlay_logo);

            // Initialize a bitmap renderer with the bitmap
            mWZBitmap = new WZBitmap(overlayBitmap);

            // Center the bitmap in the display
            mWZBitmap.setPosition(WZBitmap.CENTER, WZBitmap.CENTER);

            // Scale the bitmap initially to 75% of the display surface width
            mWZBitmap.setScale(0.75f, WZBitmap.SURFACE_WIDTH);

            // Register the bitmap renderer with the GoCoder camera preview view as a frame listener
            mWZCameraView.registerFrameRenderer(mWZBitmap);

            Toast.makeText(this, getString(R.string.bitmap_overlay_help), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Send all events to the scale gesture detector
        mScaleDetector.onTouchEvent(event);
        return true;
    }

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Get the scale factor and apply it to the bitmap's scale
            mScaleFactor *= detector.getScaleFactor();

            // Cap the scale factor
            mScaleFactor = Math.max(0.3f, Math.min(mScaleFactor, 5.0f));
            if (mWZBitmap != null)
                mWZBitmap.setScale(mScaleFactor); //, WZBitmap.SURFACE_WIDTH);

            return true;
        }
    }
}

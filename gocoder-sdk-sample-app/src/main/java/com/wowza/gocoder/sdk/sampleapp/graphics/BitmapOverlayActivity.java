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

package com.wowza.gocoder.sdk.sampleapp.graphics;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.android.graphics.WOWZBitmap;
import com.wowza.gocoder.sdk.sampleapp.CameraActivityBase;
import com.wowza.gocoder.sdk.sampleapp.R;

/**
 * This activity class demonstrates use of the WOWZBitmap API to display a bitmap
 * as an overlay within the GoCoder SDK camera preview display
 */
public class BitmapOverlayActivity extends CameraActivityBase {

    private WOWZBitmap mWZBitmap = null;
    private ScaleGestureDetector mScaleDetector = null;
    private float mScaleFactor = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overlay);

        mRequiredPermissions = new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };

        mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());
    }
    @Override
    protected void onResume() {
        super.onResume();

        // Create the initial Bitmap
        if (sGoCoderSDK != null && mWZCameraView != null && mWZBitmap == null) {
            // Read in a PNG file from the app resources as a bitmap
            Bitmap overlayBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.overlay_logo);

            // Initialize a bitmap renderer with the bitmap
            mWZBitmap = new WOWZBitmap(overlayBitmap);

            // Center the bitmap in the display
            mWZBitmap.setPosition(WOWZBitmap.CENTER, WOWZBitmap.CENTER);

            // Scale the bitmap initially to 75% of the display surface width
            mWZBitmap.setScale(0.75f, WOWZBitmap.FRAME_WIDTH);

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
                mWZBitmap.setScale(mScaleFactor); //, WOWZBitmap.SURFACE_WIDTH);

            return true;
        }
    }

    /**
     * Click handler for the broadcast button
     */
    public void onToggleBroadcast(View v) {
        super.onToggleBroadcast(v,null);
    }

    /**
     * Click handler for the Settings button
     */
    public void onSettings(View v) {
        super.onToggleBroadcast(v,null);
    }


}

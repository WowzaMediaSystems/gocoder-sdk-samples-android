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

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.android.opengl.WOWZGLES;
import com.wowza.gocoder.sdk.api.devices.WOWZCamera;
import com.wowza.gocoder.sdk.api.devices.WOWZCameraView;
import com.wowza.gocoder.sdk.api.errors.WOWZError;
import com.wowza.gocoder.sdk.api.geometry.WOWZSize;
import com.wowza.gocoder.sdk.api.logging.WOWZLog;
import com.wowza.gocoder.sdk.api.render.WOWZRenderAPI;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenshotActivity extends GoCoderSDKActivityBase {
    private final static String TAG = ScreenshotActivity.class.getSimpleName();

    // UI views and controls
    private WOWZCameraView mWZCameraView   = null;
    private ImageButton     mBtnScreenshot  = null;
    private AtomicBoolean   mGrabFrame      = new AtomicBoolean(false);
    private AtomicBoolean   mSavingFrame    = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshot);

        mRequiredPermissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        mBtnScreenshot = (ImageButton) findViewById(R.id.ic_screenshot);
        mBtnScreenshot.setEnabled(false);
        mBtnScreenshot.setClickable(false);

        if (sGoCoderSDK != null) {
            mWZCameraView = (WOWZCameraView) findViewById(R.id.camera_preview);

            // Create an register a video frame listener with WZCameraPreview
            WOWZRenderAPI.VideoFrameListener videoFrameListener = new WOWZRenderAPI.VideoFrameListener() {

                @Override
                // onWZVideoFrameListenerFrameAvailable() will only be called nce isWZVideoFrameListenerActive() returns true
                public boolean isWZVideoFrameListenerActive() {
                    // Only indicate the frame listener once the screenshot button has been pressed
                    // and when we're not in the process of saving a previous screenshot
                    return (mGrabFrame.get() && !mSavingFrame.get());
                }

                @Override
                public void onWZVideoFrameListenerInit(WOWZGLES.EglEnv eglEnv) {
                    // nothing needed for this example
                }

                @Override
                // onWZVideoFrameListenerFrameAvailable() is called when isWZVideoFrameListenerActive() = true
                // and a new frame has been rendered on the camera preview display surface
                public void onWZVideoFrameListenerFrameAvailable(WOWZGLES.EglEnv eglEnv, WOWZSize frameSize, int frameRotation, long timecodeNanos) {
                    // set these flags so that this doesn't get called numerous times in parallel
                    mSavingFrame.set(true);
                    mGrabFrame.set(false);

                    // create a pixel buffer and read the pixels from the camera preview display surface using GLES
                    final WOWZSize bitmapSize = new WOWZSize(frameSize);
                    final ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(bitmapSize.getWidth() * bitmapSize.getHeight() * 4);
                    pixelBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    GLES20.glReadPixels(0, 0, bitmapSize.getWidth(),  bitmapSize.getHeight(),
                            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer);
                    final int eglError = WOWZGLES.checkForEglError(TAG + "(glReadPixels)");
                    if (eglError != EGL14.EGL_SUCCESS) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mBtnScreenshot.setEnabled(true);
                                mBtnScreenshot.setClickable(true);
                                Toast.makeText(getApplicationContext(), WOWZGLES.getEglErrorString(eglError), Toast.LENGTH_LONG).show();
                            }
                        });

                        mSavingFrame.set(false);
                        return;
                    }
                    pixelBuffer.rewind();

                    // now that we have the pixels, create a new thread for transforming and saving the bitmap
                    // so that we don't block the camera preview display renderer
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BufferedOutputStream bitmapStream = null;
                            StringBuffer statusMessage = new StringBuffer();

                            try {
                                File jpegFile = getOutputJpegFile();
                                if (jpegFile != null) {
                                    bitmapStream = new BufferedOutputStream(new FileOutputStream(jpegFile));
                                    Bitmap bmp = Bitmap.createBitmap(bitmapSize.getWidth(), bitmapSize.getHeight(), Bitmap.Config.ARGB_8888);
                                    bmp.copyPixelsFromBuffer(pixelBuffer);

                                    Matrix xformMatrix = new Matrix();
                                    xformMatrix.setScale(-1, 1);  // flip horiz
                                    xformMatrix.preRotate(180);  // flip vert
                                    bmp = Bitmap.createBitmap(bmp, 0, 0, bitmapSize.getWidth(), bitmapSize.getHeight(), xformMatrix, false);

                                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, bitmapStream);
                                    bmp.recycle();

                                    statusMessage.append("Screenshot saved to ").append(jpegFile.getAbsolutePath());
                                } else {
                                    statusMessage.append("The directory for the screenshot could not be created");
                                }

                            } catch(Exception e) {
                                WOWZLog.error(TAG, "An exception occurred trying to create the screenshot", e);
                                statusMessage.append(e.getLocalizedMessage());

                            } finally {
                                if (bitmapStream != null) {
                                    try {
                                        bitmapStream.close();
                                    } catch (IOException closeException) {
                                        // ignore exception on close
                                    }
                                }

                                final String toastStr = statusMessage.toString();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), toastStr, Toast.LENGTH_LONG).show();
                                        mBtnScreenshot.setEnabled(true);
                                        mBtnScreenshot.setClickable(true);
                                    }
                                });

                                mSavingFrame.set(false);
                            }
                        }
                    }).start();
                }

                @Override
                public void onWZVideoFrameListenerRelease(WOWZGLES.EglEnv eglEnv) {
                    // nothing needed for this example
                }
            };

            // register our newly created video frame listener wth the camera preview display view
            mWZCameraView.registerFrameListener(videoFrameListener);

            if(mPermissionsGranted) {
                // start the camera preview display and enable the screenshot button when it is ready
                mWZCameraView.startPreview(new WOWZCameraView.PreviewStatusListener() {
                    @Override
                    public void onWZCameraPreviewStarted(WOWZCamera camera, WOWZSize frameSize, int frameRate) {
                        mBtnScreenshot.setEnabled(true);
                        mBtnScreenshot.setClickable(true);
                    }

                    @Override
                    public void onWZCameraPreviewStopped(int cameraId) {
                        mBtnScreenshot.setEnabled(false);
                        mBtnScreenshot.setClickable(false);
                    }

                    @Override
                    public void onWZCameraPreviewError(WOWZCamera camera, WOWZError error) {
                        mBtnScreenshot.setEnabled(false);
                        mBtnScreenshot.setClickable(false);
                        displayErrorDialog(error);
                    }
                });
            }
        }
    }

    public void onTakeScreenshot(View v) {
        // Setting mGrabFrame to true will trigger the video frame listener to become active
        if (!mGrabFrame.get() && !mSavingFrame.get()) {
            mBtnScreenshot.setEnabled(false);
            mBtnScreenshot.setClickable(false);
            mGrabFrame.set(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mWZCameraView != null) {
            mWZCameraView.onResume();

            if(mPermissionsGranted && !mWZCameraView.isPreviewing()) {
                // start the camera preview display and enable the screenshot button when it is ready
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        // start the camera preview display and enable the screenshot button when it is ready
                        mWZCameraView.startPreview(new WOWZCameraView.PreviewStatusListener() {
                            @Override
                            public void onWZCameraPreviewStarted(WOWZCamera camera, WOWZSize frameSize, int frameRate) {
                                mBtnScreenshot.setEnabled(true);
                                mBtnScreenshot.setClickable(true);
                            }

                            @Override
                            public void onWZCameraPreviewStopped(int cameraId) {
                                mBtnScreenshot.setEnabled(false);
                                mBtnScreenshot.setClickable(false);
                            }

                            @Override
                            public void onWZCameraPreviewError(WOWZCamera camera, WOWZError error) {
                                mBtnScreenshot.setEnabled(false);
                                mBtnScreenshot.setClickable(false);
                                displayErrorDialog(error);
                            }
                        });
                    }
                }, 300);
            }
        }
    }

    @Override
    protected void onPause() {
        if (mWZCameraView != null)
            mWZCameraView.onPause();

        super.onPause();
    }

    /**
     * Returns a File object to use for saving an image
     */
    private File getOutputJpegFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        //
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "GoCoderSDK Screenshots");

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                WOWZLog.error(TAG, "Failed to create the directory in which to store the screenshot");
                return null;
            }
        }

        // Create a media file name
        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date());

        return new File(mediaStorageDir.getPath() + File.separator + timeStamp + ".jpg");
    }
}


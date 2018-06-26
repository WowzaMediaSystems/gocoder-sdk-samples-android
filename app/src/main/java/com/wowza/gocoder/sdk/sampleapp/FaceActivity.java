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
 *  © 2015 – 2018 Wowza Media Systems, LLC. All rights reserved.
 */

package com.wowza.gocoder.sdk.sampleapp;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.wowza.gocoder.sdk.api.android.graphics.WOWZBitmap;
import com.wowza.gocoder.sdk.api.android.graphics.WOWZText;
import com.wowza.gocoder.sdk.api.android.graphics.WOWZTextManager;
import com.wowza.gocoder.sdk.api.devices.WOWZCamera;
import com.wowza.gocoder.sdk.api.geometry.WOWZSize;
import com.wowza.gocoder.sdk.sampleapp.ui.MultiStateButton;
import com.wowza.gocoder.sdk.sampleapp.ui.TimerView;

import java.util.UUID;

public class FaceActivity extends CameraActivityBase
    implements  Camera.FaceDetectionListener {

    private final static String TAG = FaceActivity.class.getSimpleName();

    // UI controls
    protected MultiStateButton      mBtnSwitchCamera  = null;
    protected MultiStateButton      mBtnTorch         = null;
    protected TimerView             mTimerView        = null;

    protected WOWZBitmap mNinjaFace        = null;
    protected boolean               mDetectingFaces   = false;
    protected WOWZText mNoFaces          = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mRequiredPermissions = new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };

        // Initialize the UI controls
        mBtnTorch           = (MultiStateButton) findViewById(R.id.ic_torch);
        mBtnSwitchCamera    = (MultiStateButton) findViewById(R.id.ic_switch_camera);
        mTimerView          = (TimerView) findViewById(R.id.txtTimer);

        if (sGoCoderSDK != null) {
            // Read in the text font and create the rotated side banner
            WOWZTextManager wzTextManager = WOWZTextManager.getInstance();
            UUID fontId = wzTextManager.loadFont("njnaruto.ttf", 75, 15, 0);

            WOWZText textObject = wzTextManager.createTextObject(fontId, "Become a Wowza Ninja", 0.98f, 0.47f, 0.11f);
            textObject.setPosition( 35, 50 );
            textObject.setRotationAngle(90);
            textObject.setAlignment( WOWZText.LEFT );

            // Create the 'No faces detected' text object
            mNoFaces = wzTextManager.createTextObject(fontId, "No faces detected", 0.98f, 0.47f, 0.11f);
            mNoFaces.setPosition( WOWZText.CENTER, WOWZText.CENTER );
            mNoFaces.setAlignment( WOWZText.CENTER );
            mNoFaces.setVisible(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sGoCoderSDK != null)
            WOWZTextManager.getInstance().clear();
    }

    @Override
    public void onWZCameraPreviewStarted(WOWZCamera wzCamera, WOWZSize frameSize, int frameRate) {
        super.onWZCameraPreviewStarted(wzCamera, frameSize, frameRate);

        if (cameraSupportsFaceDetection(wzCamera))
            mDetectingFaces = setFaceDetectionState(wzCamera, true);
        else
            Toast.makeText(this, "The selected camera does not support facial recognition", Toast.LENGTH_LONG).show();
    }

    /**
     * Checks to see if the specified camera support facial recognition
     * @param camera The camera to check
     * @return true if the specified camera supports facial recognition, false otherwise
     */
    private boolean cameraSupportsFaceDetection(WOWZCamera camera) {
        Camera nativeCamera = camera.getPlatformDevice();
        return (nativeCamera.getParameters().getMaxNumDetectedFaces() > 0);
    }

    /**
     * Turn face detection on or off
     * @param camera The active camera
     * @param turnOn specifies whether to turn face detection on or off
     * @return Indicates if face detection was turned on or off
     */
    private synchronized boolean setFaceDetectionState(WOWZCamera camera, boolean turnOn) {
        Camera nativeCamera = camera.getPlatformDevice();

        if (!turnOn) {
            nativeCamera.stopFaceDetection();
            mDetectingFaces = false;
        } else if (nativeCamera.getParameters().getMaxNumDetectedFaces() > 0) {
            nativeCamera.setFaceDetectionListener(this);
            nativeCamera.startFaceDetection();
            mDetectingFaces = true;
        }

        return mDetectingFaces;
    }

    /**
     * Callback invoked by the face detection subsystem
     * @param faces An array of faces detected
     * @param camera The active camera (Android SDK)
     */
    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {
        Camera.Face chosenFace = null;

        // Choose the face with the biggest confidence score over 50
        if (faces.length > 0) {
            chosenFace = faces[0];
            for(Camera.Face face : faces)
                if (face.score > chosenFace.score)
                    chosenFace = face;

            if (chosenFace.score < 50) chosenFace = null;
        }

        if (chosenFace != null) {
            // Calculate the face's bounding box in view coordinates
            RectF viewRect = mWZCameraView.getCamera().toViewCoords(mWZCameraView, chosenFace.rect);
            // Calculate the percentage of the screen width that the face occupies
            float faceScale = viewRect.width() / (float)mWZCameraView.getWidth();

            // Set the bitmap to the center of the face's bounding box
            mNinjaFace.setPosition((int)viewRect.centerX(), mWZCameraView.getHeight() - Math.round(viewRect.centerY()));
            // Scale the bitmap to the same size as the face
            mNinjaFace.setScale(faceScale * 1.5f, WOWZBitmap.SURFACE_WIDTH);

            //WOWZLog.debug(TAG, viewRect.toString());
        }

        mNinjaFace.setVisible(chosenFace != null);
        mNoFaces.setVisible(chosenFace == null);
    }

    /**
     * Android Activity lifecycle methods
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (sGoCoderSDK != null && mWZCameraView != null) {
            if (mNinjaFace == null) {
                // Read in the bitmap for the face detection
                Bitmap faceBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ninja_face);
                mNinjaFace = new WOWZBitmap(faceBitmap);
                mNinjaFace.setPosition(WOWZBitmap.CENTER, WOWZBitmap.CENTER);
                mNinjaFace.setScale(0.50f, WOWZBitmap.SURFACE_WIDTH);
                mNinjaFace.setVisible(false);

                mWZCameraView.registerFrameRenderer(mNinjaFace);
            }
        }
    }

    @Override
    protected void onPause() {
        if (mDetectingFaces)
            setFaceDetectionState(mWZCameraView.getCamera(), false);

        if (mNinjaFace != null)
            mNinjaFace.setVisible(false);
        if (mNoFaces != null)
            mNoFaces.setVisible(false);

        super.onPause();
    }

    /**
     * Click handler for the switch camera button
     */
    public void onSwitchCamera(View v) {
        if (mWZCameraView == null) return;

        if (mDetectingFaces)
            setFaceDetectionState(mWZCameraView.getCamera(), false);

        if (mNinjaFace != null)
            mNinjaFace.setVisible(false);
        if (mNoFaces != null)
            mNoFaces.setVisible(false);

        WOWZCamera newCamera = mWZCameraView.switchCamera();

        if (cameraSupportsFaceDetection(newCamera))
            mDetectingFaces = setFaceDetectionState(newCamera, true);
        else
            Toast.makeText(this, "The selected camera does not support facial recognition", Toast.LENGTH_LONG).show();
    }

    /**
     * Click handler for the torch/flashlight button
     */
    public void onToggleTorch(View v) {
        if (mWZCameraView == null) return;

        WOWZCamera activeCamera = mWZCameraView.getCamera();
        activeCamera.setTorchOn(mBtnTorch.toggleState());
    }

    /**
     * Update the state of the UI controls
     */
    @Override
    protected boolean syncUIControlState() {
        boolean disableControls = super.syncUIControlState();

        if (disableControls) {
            mBtnSwitchCamera.setEnabled(false);
            mBtnTorch.setEnabled(false);
        } else {
            boolean isDisplayingVideo = false;
            boolean isStreaming = getBroadcast().getStatus().isRunning();
            if(this.hasDevicePermissionToAccess()){
                isDisplayingVideo = (getBroadcastConfig().isVideoEnabled() && mWZCameraView.getCameras().length > 0);
            }


            if (isDisplayingVideo) {
                WOWZCamera activeCamera = mWZCameraView.getCamera();

                boolean hasTorch = (activeCamera != null && activeCamera.hasCapability(WOWZCamera.TORCH));
                mBtnTorch.setEnabled(hasTorch);
                if (hasTorch) {
                    mBtnTorch.setState(activeCamera.isTorchOn());
                }

                mBtnSwitchCamera.setEnabled(mWZCameraView.isSwitchCameraAvailable());
            } else {
                mBtnSwitchCamera.setEnabled(false);
                mBtnTorch.setEnabled(false);
            }

            if (isStreaming && !mTimerView.isRunning()) {
                mTimerView.startTimer();
            } else if (getBroadcast().getStatus().isIdle() && mTimerView.isRunning()) {
                mTimerView.stopTimer();
            } else if (!isStreaming) {
                mTimerView.setVisibility(View.GONE);
            }
        }

        return disableControls;
    }
}

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

import android.os.Bundle;

import com.wowza.gocoder.sdk.api.android.graphics.WZText;
import com.wowza.gocoder.sdk.api.android.graphics.WZTextManager;
import com.wowza.gocoder.sdk.sampleapp.CameraActivityBase;
import com.wowza.gocoder.sdk.sampleapp.R;

import java.util.UUID;

/**
 * This activity class demonstrates use of the WText API to display a text string
 * as an overlay within the GoCoder SDK camera preview display and resulting video stream
 */
public class TextOverlayActivity extends CameraActivityBase {
    private final static String TAG = TextOverlayActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overlay);

        if (sGoCoderSDK != null) {
            // Get the GoCoder SDK text manager
            WZTextManager wzTextManager = WZTextManager.getInstance();

            // Load a True Type font file from the app's assets folder
            UUID fontId = wzTextManager.loadFont("njnaruto.ttf", 100, 15, 0);

            // Display the text centered on the screen
            WZText textObject = wzTextManager.createTextObject(fontId, "Become a Wowza Ninja", 0.84f, 0.47f, 0f);
            textObject.setPosition( WZText.CENTER, WZText.CENTER );
            textObject.setAlignment( WZText.CENTER );
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sGoCoderSDK != null)
            WZTextManager.getInstance().clear();
    }
}

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

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import com.wowza.gocoder.sdk.sampleapp.R;

/**
 * A utility class for a multi-state toggle button
 */
public class ControlButton {

    private ImageButton imageButton = null;
    private Drawable onIcon         = null;
    private Drawable offIcon        = null;

    private boolean stateOn = false;

    private int pressedColor;

    public ControlButton(Activity activity, int resourceId, final boolean enabled) {

        this.pressedColor = activity.getResources().getColor(R.color.controlButtonPressed);
        this.imageButton = (ImageButton) activity.findViewById(resourceId);
        this.imageButton.setClickable(enabled);

        this.imageButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (!imageButton.isClickable()) return false;

                ImageButton btn = (ImageButton) v;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    btn.getDrawable().setColorFilter(pressedColor, PorterDuff.Mode.SRC_IN);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    btn.getDrawable().clearColorFilter();
                }

                return false;
            }
        });

        setEnabled(enabled);
    }

    public ControlButton(Activity activity, int resourceId, boolean enabled, boolean stateOn, int onIconId, int offIconId) {
        this(activity, resourceId, enabled);

        this.stateOn = stateOn;
        this.onIcon = activity.getResources().getDrawable(onIconId);
        this.offIcon = activity.getResources().getDrawable(offIconId);

        setStateOn(stateOn);
    }

    public boolean isEnabled() {
        return imageButton.isClickable();
    }

    public void setEnabled(boolean enabled) {
        imageButton.setClickable(enabled);
        imageButton.setImageAlpha(isEnabled() ? 255 : 125);
    }

    public boolean isVisible() {
        return (imageButton.getVisibility() == View.VISIBLE);
    }

    public void setVisible(boolean visible) {
        imageButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    public boolean toggleState() {
        if (this.onIcon == null) return false;

        this.stateOn = !this.stateOn;
        setStateOn(this.stateOn);

        return this.stateOn;
    }

    public boolean isStateOn() {
        return stateOn;
    }

    public void setStateOn(boolean stateOn) {
        if (this.onIcon == null) return;

        this.stateOn = stateOn;
        imageButton.setImageDrawable(this.stateOn ? this.onIcon : this.offIcon);
    }
}

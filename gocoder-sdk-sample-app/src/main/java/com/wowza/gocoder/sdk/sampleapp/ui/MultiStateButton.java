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
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.wowza.gocoder.sdk.sampleapp.R;

/**
 * A utility class for a multi-state toggle button
 */
public class MultiStateButton extends AppCompatImageView {

    private Drawable    mOnDrawable     = null;
    private Drawable    mOffDrawable    = null;
    private boolean     mIsOn           = false;
    private int         mPressedColor;
    private int         mDisabledColor;

    public MultiStateButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        mDisabledColor  = context.getResources().getColor(R.color.multiStateButtonDisabled);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                com.wowza.gocoder.sdk.sampleapp.R.styleable.MultiStateButton,
                0, 0);

        try {
            boolean isOn = a.getBoolean(R.styleable.MultiStateButton_isOn, true);
            Drawable offDrawable = a.getDrawable(R.styleable.MultiStateButton_offSrc);
            int pressedColor = a.getColor(R.styleable.MultiStateButton_pressedColor, context.getResources().getColor(R.color.multiStateButtonPressed));

            init(isOn, offDrawable, pressedColor);

        } finally {
            a.recycle();
        }
    }

    public MultiStateButton(Context context) {
        super(context);
        init(mIsOn, null, mPressedColor);
    }

    private void init(boolean isOn, Drawable offDrawable, int pressedColor) {
        mOnDrawable     = getDrawable();
        mOffDrawable    = (offDrawable == null ? mOnDrawable : offDrawable);
        mPressedColor   = pressedColor;


        setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                ImageView imgView = (ImageView) v;
                if (!imgView.isClickable()) return false;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    imgView.getDrawable().setColorFilter(mPressedColor, PorterDuff.Mode.SRC_IN);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    imgView.getDrawable().clearColorFilter();
                }

                return false;
            }
        });


        //setClickable(isEnabled());
        setState(isOn);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setClickable(enabled);

        if (enabled) {
            getDrawable().clearColorFilter();
        } else {
            getDrawable().setColorFilter(mDisabledColor, PorterDuff.Mode.SRC_IN);
        }

        invalidate();
        requestLayout();
    }

    public boolean isOn() {
        return mIsOn;
    }

    public void setState(boolean isOn) {
        mIsOn = isOn;

        setImageDrawable(mIsOn ? mOnDrawable : mOffDrawable);
        if (isEnabled()) {
            getDrawable().clearColorFilter();
        } else {
            getDrawable().setColorFilter(mDisabledColor, PorterDuff.Mode.SRC_IN);
        }

        invalidate();
        requestLayout();
    }

    public boolean toggleState() {
        setState(!mIsOn);
        return mIsOn;
    }
}

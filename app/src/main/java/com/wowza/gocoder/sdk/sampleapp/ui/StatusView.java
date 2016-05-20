/**
 *  This code and all components (c) Copyright 2015-2016, Wowza Media Systems, LLC. All rights reserved.
 *  This code is licensed pursuant to the BSD 3-Clause License.
 */
package com.wowza.gocoder.sdk.sampleapp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wowza.gocoder.sdk.api.errors.WZError;
import com.wowza.gocoder.sdk.api.status.WZState;
import com.wowza.gocoder.sdk.api.status.WZStatus;
import com.wowza.gocoder.sdk.sampleapp.R;

/**
 * An activity indicator-like view for display status messages and errors
 */
public class StatusView extends RelativeLayout {
    final private static String TAG = StatusView.class.getSimpleName();

    private TextView mTxtStatus;
    private Button mBtnDismiss;
    private String mStatusMessage;

    private AlphaAnimation showAnimation, hideAnimation;
    private volatile boolean isPaused;
    private volatile boolean isShowing;
    private volatile boolean isHiding;

    public StatusView(Context context) {
        super(context);
        init(context);
    }

    public StatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        isPaused = false;
        isShowing = false;
        isHiding = false;
        mStatusMessage = null;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.status_view, this, true);

        mTxtStatus = (TextView) findViewById(R.id.txtStatus);

        mBtnDismiss = (Button) findViewById(R.id.btnDismiss);
        mBtnDismiss.setVisibility(GONE);
        mBtnDismiss.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setVisibility(INVISIBLE);
                setClickable(false);
                mBtnDismiss.setVisibility(GONE);
                isPaused = false;
            }
        });

        showAnimation = new AlphaAnimation(0.0f, 1.0f);
        showAnimation.setDuration(500);
        showAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                preAnimation(true);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                postAnimation(true);
           }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        hideAnimation = new AlphaAnimation(1.0f, 0.0f);
        hideAnimation.setDuration(1500);
        hideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                preAnimation(false);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                postAnimation(false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        setClickable(false);
    }

    private synchronized void preAnimation(boolean beforeShow) {
        if (beforeShow) {
            isShowing = true;

            bringToFront();
            setClickable(true);
        } else {
            isHiding = true;
        }
    }

    private synchronized void postAnimation(boolean afterShow) {
        if (afterShow) {
            isShowing = false;
            setVisibility(VISIBLE);

            if (mStatusMessage == null) { // if message has been cleared, hide
                updateView();
            }

        } else {
            isHiding = false;

            setClickable(false);
            setVisibility(View.INVISIBLE);
            setAlpha(1f);
        }
    }

    public synchronized void setStatus(WZStatus status) {
        if (status.getLastError() != null) {
            isPaused = true;
            mStatusMessage = status.getLastError().getErrorDescription();
            updateView();
        } else if (!isPaused) {
            switch (status.getState()) {
                case WZState.IDLE:
                case WZState.RUNNING:
                    mStatusMessage = null;
                    break;

                case WZState.STARTING:
                    mStatusMessage = getResources().getString(R.string.status_connecting);
                    break;

                case WZState.READY:
                    mStatusMessage = getResources().getString(R.string.status_connected);
                    break;

                case WZState.STOPPING:
                    mStatusMessage = getResources().getString(R.string.status_disconnecting);
                    break;
            }
            updateView();
        }
    }

    public synchronized void setErrorMessage(String message) {
        setStatus(new WZStatus(WZState.IDLE, new WZError(message)));
    }

    public synchronized void showMessage(String message) {
        setErrorMessage(message);
    }

    private synchronized void updateView() {
        if (isPaused) { // show error
            clearAnimation();
            mTxtStatus.setText(mStatusMessage);
            bringToFront();
            setClickable(true);
            setVisibility(VISIBLE);
            setAlpha(1f);
            mBtnDismiss.setVisibility(VISIBLE);
        } else if (mStatusMessage != null) { // show message
            mTxtStatus.setText(mStatusMessage);
            if (isHiding) {
                clearAnimation();
            }
            if (!isShowing){
                startAnimation(showAnimation);
            }
        } else { // hide
            if (!isShowing && !isHiding) {
                startAnimation(hideAnimation);
            }
        }
    }
}

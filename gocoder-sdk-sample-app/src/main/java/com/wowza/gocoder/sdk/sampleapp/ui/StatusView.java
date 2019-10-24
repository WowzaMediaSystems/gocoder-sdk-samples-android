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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wowza.gocoder.sdk.api.errors.WOWZError;
import com.wowza.gocoder.sdk.support.status.WOWZState;
import com.wowza.gocoder.sdk.support.status.WOWZStatus;
import com.wowza.gocoder.sdk.sampleapp.R;

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

    public synchronized void setStatus(WOWZStatus status) {
        if (status.getLastError() != null) {
            isPaused = true;
            mStatusMessage = status.getLastError().getErrorDescription();
            updateView();
        } else if (!isPaused) {
            switch (status.getState()) {
                case WOWZState.IDLE:
                case WOWZState.RUNNING:
                case WOWZState.PAUSED:
                case WOWZState.STOPPED:
                case WOWZState.COMPLETE:
                case WOWZState.SHUTDOWN:
                case WOWZState.UNKNOWN:
                    mStatusMessage = null;
                    break;

                case WOWZState.STARTING:
                    mStatusMessage = getResources().getString(R.string.status_connecting);
                    break;

                case WOWZState.READY:
                    mStatusMessage = getResources().getString(R.string.status_connected);
                    break;

                case WOWZState.STOPPING:
                    mStatusMessage = getResources().getString(R.string.status_disconnecting);
                    break;

                case WOWZState.ERROR:
                    WOWZError err = status.getLastError();
                    mStatusMessage = (err != null ? err.getErrorDescription() : "An error occurred.");
                    break;
            }
            updateView();
        }
    }

    public synchronized void setErrorMessage(String message) {
        setStatus(new WOWZStatus(WOWZState.IDLE, new WOWZError(message)));
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

package com.wowza.gocoder.sdk.sampleapp.ui;
/**
 *  This code and all components (c) Copyright 2015-2016, Wowza Media Systems, LLC. All rights reserved.
 *  This code is licensed pursuant to the BSD 3-Clause License.
 */
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A timer control that displays a running counter of the elapsed time during a live streaming broadcast
 */
public class TimerView extends TextView {
    final public static long DEFAULT_REFRESH_INTERVAL = 1000L;

    private long mTimerStart = 0L;
    private ScheduledExecutorService mTimerThread = null;

    public TimerView(Context context) {
        super(context);
    }

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void startTimer() {
        startTimer(DEFAULT_REFRESH_INTERVAL);
    }

    public synchronized void startTimer(long refreshInterval) {
        if (mTimerThread != null) return;

        setText("00:00:00");

        mTimerStart = System.currentTimeMillis();
        mTimerThread = Executors.newSingleThreadScheduledExecutor();
        mTimerThread.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        int elapsedTime = (int) ((System.currentTimeMillis() - mTimerStart) / 1000);
                        long hours = elapsedTime / 3600L,
                                minutes = (elapsedTime / 60L) % 60L,
                                seconds = elapsedTime % 60L;

                        setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                    }
                });
            }
        }, refreshInterval, refreshInterval, TimeUnit.MILLISECONDS);

        setVisibility(VISIBLE);
    }

    public synchronized void stopTimer() {
        if (mTimerThread == null) return;

        mTimerThread.shutdown();
        mTimerThread = null;

        setVisibility(INVISIBLE);
        setText("00:00:00");
    }

    public synchronized boolean isRunning() {
        return (mTimerThread != null);
    }
}

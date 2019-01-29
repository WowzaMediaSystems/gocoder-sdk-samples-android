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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.wowza.gocoder.sdk.api.configuration.WOWZMediaConfig;
import com.wowza.gocoder.sdk.api.devices.WOWZAudioDevice;
import com.wowza.gocoder.sdk.sampleapp.audio.VUMeter;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A custom audio level meter view that demonstrates the use of the GoCoder SDK WOWZAudioDevice.AudioSampleListener interface
 */
public class AudioLevelMeter extends View
    implements WOWZAudioDevice.AudioSampleListener {

    private final String TAG = AudioLevelMeter.class.getSimpleName();

    public static final int  HANDLE_UPDATE          = 0x01;
    private static final int UPDATE_INTERVAL        = 500; // milliseconds

    public static final int DEFAULT_SQUARE_PADDING  = 10;
    public static final int DEFAULT_SQUARE_WIDTH    = 25;
    public static final int DEFAULT_SQUARE_HEIGHT   = 20;

    private static final int ANIMATION_INTERVAL     = 70;

    private static final int NUM_STEPS = 8;
    private static final int[] STEP_COLORS = new int[] {
            0xff80f280,
            0xff80f280,
            0xfffff280,
            0xfffff280,
            0xffff900e,
            0xffff900e,
            0xffff2e0e,
            0xffff2e0e
    };

    private static final int ALPHA_PEAK              = 180;
    private static final int ALPHA_LAST_PEAK         = 30;
    private static final int ALPHA_BEFORE_LAST_PEAK  = 90;

    private ScheduledExecutorService mUpdateThread;

    private int mPeak;
    private int mLastPeak;
    private int mBeforeLastPeak;

    private int mSquareWidth;
    private int mSquareHeight;
    private int mSquarePadding;

    private Paint mPaint;

    private VUMeter mVUMeter;
    private boolean mPaused;

    public UpdateHandler getHandler() {
        return mHandler;
    }
    private UpdateHandler mHandler;

    public AudioLevelMeter(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioLevelMeter(Context context) {
        super(context);
        init();
    }

    private void init() {
        mHandler        = new UpdateHandler(this);
        mUpdateThread   = null;

        mPaint          = new Paint(Paint.ANTI_ALIAS_FLAG);

        mPeak           = 0;
        mLastPeak       = 0;
        mBeforeLastPeak = 0;

        mSquarePadding  = DEFAULT_SQUARE_PADDING;
        mSquareHeight   = DEFAULT_SQUARE_HEIGHT;
        mSquareWidth    = DEFAULT_SQUARE_WIDTH;

        mVUMeter        = new VUMeter();
        mPaused         = false;

        setViewSize();
    }

    @Override
    public void onMeasure(int w, int h) {
        int x = NUM_STEPS*(mSquareWidth + mSquarePadding);;

        setMeasuredDimension(x, mSquareHeight + (2 * mSquarePadding));
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for(int i = 0; i < NUM_STEPS; i++) {
            mPaint.setColor(STEP_COLORS[i]);
            
            int x, y;
            x = (i * mSquareWidth) + (i * mSquarePadding);
            y = mSquarePadding;

            if (i >= mPeak) {
                if (i >= mLastPeak) {
                    mPaint.setAlpha(ALPHA_LAST_PEAK);
                } else {
                    if(i >= mBeforeLastPeak) {
                        mPaint.setAlpha(ALPHA_BEFORE_LAST_PEAK);
                    } else {
                        mPaint.setAlpha(ALPHA_PEAK);
                    }
                }
            }

            canvas.drawRect(x, y, x + mSquareWidth, y + mSquareHeight, mPaint);
        }

        mBeforeLastPeak = mLastPeak;
        mLastPeak = mPeak;
        
        postInvalidateDelayed(ANIMATION_INTERVAL);
    }

    private void setViewSize() {
        int viewWidth   = (mSquareWidth*NUM_STEPS) + (mSquarePadding*(NUM_STEPS-1));
        int viewHeight  = mSquareHeight;

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(viewWidth, viewHeight);
        setLayoutParams(params);
    }

    /**
     * Indicates if audio samples should be sent to the onWZAudioSampleRecorded method or not
     * @return true if onWZAudioSampleRecorded should be called with new audio sample data, false otherwise
     */
    @Override
    public boolean isWZAudioSampleListenerEnabled() {
        return true;
    }

    /**
     * This method will be called just before streaming begins
     * @param audioConfig The broadcast stream configuration
     */
    @Override
    public void onWZAudioSampleListenerSetup(WOWZMediaConfig audioConfig) {
        mPeak           = 0;
        mLastPeak       = 0;
        mBeforeLastPeak = 0;
        mPaused         = false;

        mUpdateThread = Executors.newSingleThreadScheduledExecutor();
        mUpdateThread.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (mPaused) return;

                int level = (int) ((mVUMeter.getRmsDB() / mVUMeter.getMaxDB()) * NUM_STEPS);
                int peak = (int) ((mVUMeter.getPeakDB() / mVUMeter.getMaxDB()) * NUM_STEPS);
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_UPDATE, level, peak));
            }
        }, UPDATE_INTERVAL, UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * This method is called with a buffer containing the audio sample data
     * @param sampleData The raw audio sample data
     * @param sampleDataLength The length of the data in bytes
     * @param timecodeNanos The timecode of the sample in nanoseconds
     */
    @Override
    public synchronized void onWZAudioSampleRecorded(byte[] sampleData, int sampleDataLength, long timecodeNanos) {
        short[] shorts = new short[sampleData.length/2];
        ByteBuffer.wrap(sampleData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        mVUMeter.calculateVULevels(shorts);

        post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    /**
     * This method is called is the audio stream is paused or unpaused during a broadcast
     * @param paused A flag indicating whether the audio stream is paused or unpaused
     */
    @Override
    public void onWZAudioPaused(boolean paused) {
        mPaused = paused;
        setVisibility(paused ? View.GONE : View.VISIBLE);
    }

    /**
     * This method is called when the audio stream broadcast has been stopped
     */
    @Override
    public void onWZAudioSampleListenerRelease() {
        mUpdateThread.shutdown();
        mUpdateThread = null;
    }

    private void handleUpdate(int level, int peak) {
        mPeak = Math.min(peak, NUM_STEPS-1);
        invalidate();
    }

    public static class UpdateHandler extends Handler {
        private final static String TAG = UpdateHandler.class.getSimpleName();

        private WeakReference<AudioLevelMeter> mWeakAudioMeter;

        public UpdateHandler(AudioLevelMeter audioLevelMeter) {
            mWeakAudioMeter = new WeakReference<AudioLevelMeter>(audioLevelMeter);
        }

        @Override
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;

            AudioLevelMeter audioLevelMeter = mWeakAudioMeter.get();
            if (audioLevelMeter == null) {
                Log.w(TAG, "UpdateHandler.handleMessage: audioMeter is null");
                return;
            }

            switch (what) {
                case HANDLE_UPDATE:
                    audioLevelMeter.handleUpdate(inputMessage.arg1, inputMessage.arg2);
                    break;

                default:
                    break;
            }
        }
    }

}

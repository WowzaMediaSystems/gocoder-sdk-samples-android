/*
 *
 * WOWZA MEDIA SYSTEMS, LLC ("Wowza") CONFIDENTIAL
 * © 2005 – 2019 Wowza Media Systems, LLC. All rights reserved.
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

package com.wowza.gocoder.sdk.sampleapp.ui;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;

public class VolumeChangeObserver extends ContentObserver {
    int previousVolume = 0;

    int mStreamType = AudioManager.STREAM_MUSIC;
    Context mContext = null;
    private VolumeChangeListener mVolumeChangeListener = null;

    public int getStreamType() {
        return mStreamType;
    }

    public void setStreamType(int streamType) {
        mStreamType = streamType;
    }

    public void setVolumeChangeListener(VolumeChangeListener volumeChangeListener) {
        mVolumeChangeListener = volumeChangeListener;
    }

    public void clearVolumeChangeListener() {
        mVolumeChangeListener = null;
    }

    public interface VolumeChangeListener {
        void onVolumeChanged(int previousLevel, int currentLevel);
    }

    public VolumeChangeObserver(Context c, Handler handler) {
        super(handler);
        mContext = c;

        AudioManager audio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        previousVolume = normalizedLevel(audio, audio.getStreamVolume(mStreamType), mStreamType);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        AudioManager audio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = normalizedLevel(audio, audio.getStreamVolume(mStreamType), mStreamType);

        int delta=previousVolume-currentVolume;
        if (delta != 0) {
            previousVolume = currentVolume;
            if (mVolumeChangeListener != null) {
                mVolumeChangeListener.onVolumeChanged(previousVolume, currentVolume);
            }
        }

        previousVolume=currentVolume;
    }

    private int normalizedLevel(AudioManager audioManager, int level, int streamType) {
        float maxLevel = (float)audioManager.getStreamMaxVolume(streamType);
        if (maxLevel != 0f)
            return Math.round(((float)audioManager.getStreamVolume(streamType) / maxLevel) * 100f);

        return level;
    }

}
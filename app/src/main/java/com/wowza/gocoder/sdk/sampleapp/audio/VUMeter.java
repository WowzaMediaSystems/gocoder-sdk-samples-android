/**
 * Copyright 1999-2015 Carnegie Mellon University.
 * Portions Copyright 2002-2008 Sun Microsystems, Inc.
 * Portions Copyright 2002-2008 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2013-2015 Alpha Cephei, Inc.
 *
 * All Rights Reserved.  Use is subject to license terms.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 *
 * 3. Original authors' names are not deleted.
 *
 * 4. The authors' names are not used to endorse or promote products
 * derived from this software without specific prior written
 * permission.
 *
 * This work was supported in part by funding from the Defense Advanced
 * Research Projects Agency and the National Science Foundation of the
 * United States of America, the CMU Sphinx Speech Consortium, and
 * Sun Microsystems, Inc.
 *
 * CARNEGIE MELLON UNIVERSITY, SUN MICROSYSTEMS, INC., MITSUBISHI
 * ELECTRONIC RESEARCH LABORATORIES AND THE CONTRIBUTORS TO THIS WORK
 * DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL
 * CARNEGIE MELLON UNIVERSITY, SUN MICROSYSTEMS, INC., MITSUBISHI
 * ELECTRONIC RESEARCH LABORATORIES NOR THE CONTRIBUTORS BE LIABLE FOR
 * ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
 * OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.wowza.gocoder.sdk.sampleapp.audio;

public class VUMeter {

    private double rms;
    private double average;
    private double peak;

    private static final double log10 = Math.log(10.0);
    private static final double maxDB = Math.max(0.0, 20.0 * Math.log(Short.MAX_VALUE) / log10);

    private final int peakHoldTime = 1000;
    private long then = System.currentTimeMillis();

    private final float a2 = -1.9556f;
    private final float a3 = 0.9565f;

    private final float b1 = 0.9780f;
    private final float b2 = -1.9561f;
    private final float b3 = 0.9780f;


    public final synchronized double getRmsDB() {
        return Math.max(0.0, 20.0 * Math.log(rms) / log10);
    }


    public final synchronized double getAverageDB() {
        return Math.max(0.0, 20.0 * Math.log(average) / log10);
    }


    public final synchronized double getPeakDB() {
        return Math.max(0.0, 20.0 * Math.log(peak) / log10);
    }


    public final synchronized boolean getIsClipping() {
        return (Short.MAX_VALUE) < (2 * peak);
    }


    public final synchronized double getMaxDB() {
        return maxDB;
    }

    public void calculateVULevels(byte[] data, int offset, int cnt) {
        short[] samples = new short[cnt / 2];
        for (int i = 0; i < cnt / 2; i++) {
            int o = offset + (2 * i);
            samples[i] = (short) ((data[o] << 8) | (0x000000FF & data[o + 1]));
        }
        calculateVULevels(samples);
    }

    public synchronized void calculateVULevels(double[] samples) {
        double energy = 0.0;
        average = 0.0;

        double y1 = 0.0f;
        double y2 = 0.0f;


        for (int i = 0; i < samples.length; i++) {

            // remove the DC offset with a filter

            double i1 = samples[i];
            double j = 0;
            double k = 0;

            if (i > 0) {
                j = samples[i - 1];
            }
            if (i > 1) {
                k = samples[i - 2];
            }

            double y = b1 * i1 + b2 * j + b3 * k - a2 * y1 - a3 * y2;

            y2 = y1;
            y1 = y;

            double v2 = Math.abs(y);

            long now = System.currentTimeMillis();

            energy += v2 * v2;
            average += v2;

            if (v2 > peak) {
                peak = v2;
            } else if ((now - then) > peakHoldTime) {
                peak = v2;
                then = now;
            }

        }

        rms = energy / samples.length;
        rms = Math.sqrt(rms);
        average /= samples.length;
    }

    public synchronized void calculateVULevels(short[] samples) {

        double energy = 0.0;
        average = 0.0;

        double y1 = 0.0f;
        double y2 = 0.0f;


        for (int i = 0; i < samples.length; i++) {

            // remove the DC offset with a filter

            short i1 = samples[i];
            double j = 0;
            double k = 0;

            if (i > 0) {
                j = samples[i - 1];
            }
            if (i > 1) {
                k = samples[i - 2];
            }

            double y = b1 * i1 + b2 * j + b3 * k - a2 * y1 - a3 * y2;

            y2 = y1;
            y1 = y;

            double v2 = Math.abs(y);

            long now = System.currentTimeMillis();

            energy += v2 * v2;
            average += v2;

            if (v2 > peak) {
                peak = v2;
            } else if ((now - then) > peakHoldTime) {
                peak = v2;
                then = now;
            }

        }

        rms = energy / samples.length;
        rms = Math.sqrt(rms);
        average /= samples.length;
    }
}

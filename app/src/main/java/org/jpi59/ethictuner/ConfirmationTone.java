/* Copyright (C) 2026 jpi59. SPDX-License-Identifier: GPL-3.0-or-later */
package org.jpi59.ethictuner;

/** A short locally synthesised confirmation tone; no sound file is bundled or fetched. */
public final class ConfirmationTone {
    public static final int SAMPLE_RATE_HZ = 44_100;
    private static final int DURATION_MS = 70;
    private static final int EDGE_MS = 6;
    private static final double FREQUENCY_HZ = 1_046.5;

    private ConfirmationTone() { }

    public static short[] createPcm() {
        int sampleCount = SAMPLE_RATE_HZ * DURATION_MS / 1_000;
        int edgeSamples = SAMPLE_RATE_HZ * EDGE_MS / 1_000;
        short[] samples = new short[sampleCount];
        for (int index = 0; index < sampleCount; index++) {
            double envelope = Math.min(1d, Math.min((double) index / edgeSamples,
                    (double) (sampleCount - 1 - index) / edgeSamples));
            samples[index] = (short) Math.round(Math.sin(2d * Math.PI * FREQUENCY_HZ * index / SAMPLE_RATE_HZ)
                    * envelope * 0.42d * Short.MAX_VALUE);
        }
        return samples;
    }
}

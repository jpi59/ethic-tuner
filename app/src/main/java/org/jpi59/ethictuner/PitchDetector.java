/* Copyright (C) 2026 jpi59. SPDX-License-Identifier: GPL-3.0-or-later */
package org.jpi59.ethictuner;

/**
 * Local YIN pitch detector. It returns no result for quiet or ambiguous audio
 * instead of presenting a potentially misleading note as certain.
 */
public final class PitchDetector {
    public static final double MIN_FREQUENCY_HZ = 27.0;
    public static final double MAX_FREQUENCY_HZ = 3500.0;
    private static final double MIN_RMS = 0.006;
    private static final double YIN_THRESHOLD = 0.15;
    private static final double MIN_CONFIDENCE = 0.75;
    private double[] difference = new double[0];
    private double[] cmnd = new double[0];

    public Result detect(short[] samples, int count, int sampleRate) {
        if (samples == null || count < 128 || sampleRate <= 0) return null;
        int usable = Math.min(count, samples.length);
        int maxLag = Math.min(usable / 2, (int) (sampleRate / MIN_FREQUENCY_HZ));
        int minLag = Math.max(2, (int) Math.floor(sampleRate / MAX_FREQUENCY_HZ));
        if (maxLag <= minLag + 2) return null;
        ensureCapacity(maxLag + 1);

        double mean = 0;
        for (int i = 0; i < usable; i++) mean += samples[i];
        mean /= usable;
        double energy = 0;
        for (int i = 0; i < usable; i++) { double value = samples[i] - mean; energy += value * value; }
        double rms = Math.sqrt(energy / usable) / Short.MAX_VALUE;
        if (rms < MIN_RMS) return null;

        difference[0] = 0;
        cmnd[0] = 1;
        double cumulative = 0;
        for (int lag = 1; lag <= maxLag; lag++) {
            double sum = 0;
            for (int i = 0; i < usable - lag; i++) {
                double delta = samples[i] - samples[i + lag];
                sum += delta * delta;
            }
            difference[lag] = sum;
            cumulative += sum;
            cmnd[lag] = cumulative == 0 ? 1 : sum * lag / cumulative;
        }

        int candidate = -1;
        for (int lag = minLag; lag < maxLag; lag++) {
            if (cmnd[lag] < YIN_THRESHOLD && cmnd[lag] <= cmnd[lag + 1]) {
                candidate = lag;
                break;
            }
        }
        if (candidate < 0) {
            double lowest = Double.MAX_VALUE;
            for (int lag = minLag; lag <= maxLag; lag++) {
                if (cmnd[lag] < lowest) { lowest = cmnd[lag]; candidate = lag; }
            }
        }
        double confidence = 1.0 - cmnd[candidate];
        if (confidence < MIN_CONFIDENCE) return null;

        double refinedLag = candidate;
        if (candidate > minLag && candidate < maxLag) {
            double before = cmnd[candidate - 1], centre = cmnd[candidate], after = cmnd[candidate + 1];
            double denominator = before - 2 * centre + after;
            if (Math.abs(denominator) > 1e-12) {
                refinedLag += Math.max(-0.5, Math.min(0.5, 0.5 * (before - after) / denominator));
            }
        }
        return new Result(sampleRate / refinedLag, confidence, rms);
    }

    private void ensureCapacity(int size) {
        if (difference.length >= size) return;
        difference = new double[size];
        cmnd = new double[size];
    }

    public static final class Result {
        public final double frequencyHz;
        public final double confidence;
        public final double rms;

        Result(double frequencyHz, double confidence, double rms) {
            this.frequencyHz = frequencyHz;
            this.confidence = confidence;
            this.rms = rms;
        }
    }
}

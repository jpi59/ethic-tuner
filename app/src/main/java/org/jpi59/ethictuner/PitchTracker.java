/* Copyright (C) 2026 jpi59. SPDX-License-Identifier: GPL-3.0-or-later */
package org.jpi59.ethictuner;

/**
 * Turns independent pitch estimates into an honest, time-aware display state.
 *
 * <p>A missing or contradictory analysis frame is not evidence that a played note vanished.
 * This tracker therefore holds the last accepted result briefly, while explicitly reporting
 * that the signal is weak or ambiguous. It never invents a new frequency during that period.</p>
 */
public final class PitchTracker {
    public enum State { STABLE, HELD, AMBIGUOUS, NONE }

    private static final long HOLD_MILLIS = 550;
    private static final int HISTORY_SIZE = 5;
    private static final int REQUIRED_ACQUISITION_FRAMES = 2;
    private static final int REQUIRED_NEW_PITCH_FRAMES = 3;
    private static final double JUMP_CENTS = 150.0;
    private static final double PENDING_SPREAD_CENTS = 35.0;
    private static final double FILTER_WEIGHT = .45;

    private final double[] history = new double[HISTORY_SIZE];
    private int historyCount;
    private int historyNext;
    private double trackedHz;
    private double lastConfidence;
    private double lastRms;
    private long lastAcceptedAt = Long.MIN_VALUE;
    private double pendingHz;
    private int pendingCount;
    private double acquisitionHz;
    private int acquisitionCount;

    public Frame update(PitchDetector.Result result, long nowMillis) {
        if (result == null) return missing(nowMillis);
        if (trackedHz == 0 && !acquire(result.frequencyHz)) {
            return new Frame(State.NONE, 0, 0, 0, 0);
        }
        if (trackedHz > 0 && Math.abs(centsBetween(result.frequencyHz, trackedHz)) > JUMP_CENTS) {
            if (pendingCount == 0 || Math.abs(centsBetween(result.frequencyHz, pendingHz)) > PENDING_SPREAD_CENTS) {
                pendingHz = result.frequencyHz;
                pendingCount = 1;
            } else {
                pendingHz = (pendingHz * pendingCount + result.frequencyHz) / (pendingCount + 1);
                pendingCount++;
            }
            if (pendingCount < REQUIRED_NEW_PITCH_FRAMES) {
                if (age(nowMillis) > HOLD_MILLIS) {
                    reset();
                    return new Frame(State.NONE, 0, 0, 0, 0);
                }
                return new Frame(State.AMBIGUOUS, trackedHz, lastConfidence, lastRms, age(nowMillis));
            }
            clearHistory();
            trackedHz = 0;
        }

        pendingCount = 0;
        add(result.frequencyHz);
        double median = median();
        trackedHz = trackedHz == 0 ? median : FILTER_WEIGHT * median + (1 - FILTER_WEIGHT) * trackedHz;
        lastConfidence = result.confidence;
        lastRms = result.rms;
        lastAcceptedAt = nowMillis;
        return new Frame(State.STABLE, trackedHz, lastConfidence, lastRms, 0);
    }

    private boolean acquire(double frequencyHz) {
        if (acquisitionCount == 0 || Math.abs(centsBetween(frequencyHz, acquisitionHz)) > PENDING_SPREAD_CENTS) {
            acquisitionHz = frequencyHz;
            acquisitionCount = 1;
            return false;
        }
        acquisitionHz = (acquisitionHz * acquisitionCount + frequencyHz) / (acquisitionCount + 1);
        acquisitionCount++;
        if (acquisitionCount < REQUIRED_ACQUISITION_FRAMES) return false;
        acquisitionCount = 0;
        return true;
    }

    public void reset() {
        clearHistory();
        trackedHz = 0;
        lastConfidence = 0;
        lastRms = 0;
        lastAcceptedAt = Long.MIN_VALUE;
        pendingCount = 0;
        pendingHz = 0;
        acquisitionCount = 0;
        acquisitionHz = 0;
    }

    private Frame missing(long nowMillis) {
        if (trackedHz == 0) {
            acquisitionCount = 0;
            acquisitionHz = 0;
        }
        if (trackedHz > 0 && age(nowMillis) <= HOLD_MILLIS) {
            return new Frame(State.HELD, trackedHz, lastConfidence, lastRms, age(nowMillis));
        }
        if (trackedHz > 0) reset();
        return new Frame(State.NONE, 0, 0, 0, 0);
    }

    private long age(long nowMillis) {
        return lastAcceptedAt == Long.MIN_VALUE ? Long.MAX_VALUE : Math.max(0, nowMillis - lastAcceptedAt);
    }

    private void add(double value) {
        history[historyNext] = value;
        historyNext = (historyNext + 1) % HISTORY_SIZE;
        if (historyCount < HISTORY_SIZE) historyCount++;
    }

    private double median() {
        double[] values = new double[historyCount];
        System.arraycopy(history, 0, values, 0, historyCount);
        java.util.Arrays.sort(values);
        int middle = values.length / 2;
        return values.length % 2 == 0 ? (values[middle - 1] + values[middle]) / 2 : values[middle];
    }

    private void clearHistory() {
        historyCount = 0;
        historyNext = 0;
    }

    private static double centsBetween(double newer, double older) {
        return 1200 * Math.log(newer / older) / Math.log(2);
    }

    public static final class Frame {
        public final State state;
        public final double frequencyHz;
        public final double confidence;
        public final double rms;
        public final long ageMillis;

        Frame(State state, double frequencyHz, double confidence, double rms, long ageMillis) {
            this.state = state;
            this.frequencyHz = frequencyHz;
            this.confidence = confidence;
            this.rms = rms;
            this.ageMillis = ageMillis;
        }
    }
}

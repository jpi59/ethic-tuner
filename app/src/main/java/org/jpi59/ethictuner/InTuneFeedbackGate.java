/* Copyright (C) 2026 jpi59. SPDX-License-Identifier: GPL-3.0-or-later */
package org.jpi59.ethictuner;

/** Limits the local confirmation cue to a genuine, settled arrival at the target pitch. */
public final class InTuneFeedbackGate {
    private static final long MINIMUM_CUE_INTERVAL_MS = 800;
    private boolean wasInTune;
    private long lastCueAt = Long.MIN_VALUE;

    public boolean shouldPlay(boolean isInTune, long nowMs) {
        boolean shouldPlay = isInTune && !wasInTune
                && (lastCueAt == Long.MIN_VALUE || nowMs - lastCueAt >= MINIMUM_CUE_INTERVAL_MS);
        wasInTune = isInTune;
        if (shouldPlay) lastCueAt = nowMs;
        return shouldPlay;
    }

    public void reset() {
        wasInTune = false;
        lastCueAt = Long.MIN_VALUE;
    }
}

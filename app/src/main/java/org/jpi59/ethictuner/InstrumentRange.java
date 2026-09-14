/* Copyright (C) 2026 jpi59. SPDX-License-Identifier: GPL-3.0-or-later */
package org.jpi59.ethictuner;

/** Honest display limits for instrument presets; chromatic mode remains unrestricted. */
public final class InstrumentRange {
    private static final int GUITAR = 1;
    // Allows common drop-C tuning (65.4 Hz) while rejecting the low mechanical rumble seen
    // below the useful range of a standard guitar.
    private static final double GUITAR_MIN_HZ = 60.0;
    private static final double GUITAR_MAX_HZ = 1_400.0;

    private InstrumentRange() { }

    public static boolean accepts(int instrument, double frequencyHz) {
        if (!Double.isFinite(frequencyHz) || frequencyHz <= 0) return false;
        return instrument != GUITAR || (frequencyHz >= GUITAR_MIN_HZ && frequencyHz <= GUITAR_MAX_HZ);
    }
}

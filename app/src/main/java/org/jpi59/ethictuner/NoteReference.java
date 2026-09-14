/* Copyright (C) 2026 jpi59. SPDX-License-Identifier: GPL-3.0-or-later */
package org.jpi59.ethictuner;

/** Equal-temperament frequency references calculated from the selected A4 calibration. */
public final class NoteReference {
    private NoteReference() { }

    public static double frequencyHz(int midi, int a4Hz) {
        return a4Hz * Math.pow(2d, (midi - 69) / 12d);
    }
}

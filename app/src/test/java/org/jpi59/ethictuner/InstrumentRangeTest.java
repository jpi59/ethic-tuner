package org.jpi59.ethictuner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class InstrumentRangeTest {
    @Test public void guitarRejectsLowMechanicalRumbleButKeepsDropCTuning() {
        assertFalse(InstrumentRange.accepts(1, 36.7));
        assertTrue(InstrumentRange.accepts(1, 65.4));
        assertTrue(InstrumentRange.accepts(1, 82.4));
    }

    @Test public void chromaticModeRetainsTheFullDetectorRange() {
        assertTrue(InstrumentRange.accepts(0, 36.7));
    }
}

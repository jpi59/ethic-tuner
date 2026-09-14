package org.jpi59.ethictuner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ConfirmationToneTest {
    @Test public void createsAShortFadedLocalTone() {
        short[] samples = ConfirmationTone.createPcm();
        assertEquals(3_087, samples.length);
        assertEquals(0, samples[0]);
        assertEquals(0, samples[samples.length - 1]);
        boolean containsAudibleSample = false;
        for (short sample : samples) if (sample != 0) { containsAudibleSample = true; break; }
        assertTrue(containsAudibleSample);
    }
}

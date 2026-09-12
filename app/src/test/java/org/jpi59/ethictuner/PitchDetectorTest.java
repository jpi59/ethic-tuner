package org.jpi59.ethictuner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PitchDetectorTest {
    private static final int RATE = 44100;

    @Test public void estimatesReferenceTonesAcrossSupportedRange() {
        for (double expected : new double[]{27.5, 55.0, 110.0, 440.0, 1760.0, 3000.0}) {
            PitchDetector.Result result = new PitchDetector().detect(sine(expected, 12000), 4096, RATE);
            assertNotNull("Expected a pitch for " + expected + " Hz", result);
            double cents = 1200 * Math.log(result.frequencyHz / expected) / Math.log(2);
            assertTrue("Expected < 5 cents for " + expected + " Hz but was " + cents, Math.abs(cents) < 5.0);
            assertTrue(result.confidence >= .75);
        }
    }

    @Test public void rejectsSilenceAndUncorrelatedNoise() {
        short[] silence = new short[4096];
        assertNull(new PitchDetector().detect(silence, silence.length, RATE));
        short[] noise = new short[4096]; long state = 42;
        for (int i = 0; i < noise.length; i++) { state = state * 1103515245 + 12345; noise[i] = (short) (state >>> 16); }
        assertNull(new PitchDetector().detect(noise, noise.length, RATE));
    }

    private short[] sine(double hz, int amplitude) {
        short[] data = new short[4096];
        for (int i = 0; i < data.length; i++) data[i] = (short) Math.round(amplitude * Math.sin(2 * Math.PI * hz * i / RATE));
        return data;
    }
}

package org.jpi59.ethictuner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class InTuneFeedbackGateTest {
    @Test public void playsOnlyWhenTheReadingEntersTheInTuneWindow() {
        InTuneFeedbackGate gate = new InTuneFeedbackGate();
        assertFalse(gate.shouldPlay(false, 0));
        assertTrue(gate.shouldPlay(true, 100));
        assertFalse(gate.shouldPlay(true, 900));
        assertFalse(gate.shouldPlay(false, 950));
        assertTrue(gate.shouldPlay(true, 1_000));
    }

    @Test public void suppressesRapidReentryButAllowsANewSettledArrival() {
        InTuneFeedbackGate gate = new InTuneFeedbackGate();
        assertTrue(gate.shouldPlay(true, 100));
        assertFalse(gate.shouldPlay(false, 200));
        assertFalse(gate.shouldPlay(true, 700));
        assertFalse(gate.shouldPlay(false, 800));
        assertTrue(gate.shouldPlay(true, 901));
    }
}

package org.jpi59.ethictuner;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class NoteReferenceTest {
    @Test public void calculatesCommonEqualTemperamentReferences() {
        assertEquals(440d, NoteReference.frequencyHz(69, 440), 0.000001d);
        assertEquals(261.625565d, NoteReference.frequencyHz(60, 440), 0.000001d);
        assertEquals(880d, NoteReference.frequencyHz(81, 440), 0.000001d);
    }

    @Test public void followsTheSelectedCalibration() {
        assertEquals(442d, NoteReference.frequencyHz(69, 442), 0.000001d);
    }
}

package org.jpi59.ethictuner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PitchTrackerTest {
    private static PitchDetector.Result result(double hz) {
        return new PitchDetector.Result(hz, .9, .1);
    }

    private static void acquire(PitchTracker tracker, double hz, long startMillis) {
        assertEquals(PitchTracker.State.NONE, tracker.update(result(hz), startMillis).state);
        assertEquals(PitchTracker.State.STABLE, tracker.update(result(hz), startMillis + 100).state);
    }

    @Test public void holdsTheLastHonestReadingAcrossAShortDropout() {
        PitchTracker tracker = new PitchTracker();
        acquire(tracker, 440, 1_000);
        PitchTracker.Frame held = tracker.update(null, 1_500);
        assertEquals(PitchTracker.State.HELD, held.state);
        assertEquals(440, held.frequencyHz, .01);
        assertEquals(PitchTracker.State.NONE, tracker.update(null, 1_700).state);
    }

    @Test public void requiresRepeatedEvidenceBeforeAcceptingALargeJump() {
        PitchTracker tracker = new PitchTracker();
        acquire(tracker, 440, 1_000);
        assertEquals(PitchTracker.State.AMBIGUOUS, tracker.update(result(880), 1_200).state);
        assertEquals(PitchTracker.State.AMBIGUOUS, tracker.update(result(880), 1_300).state);
        PitchTracker.Frame accepted = tracker.update(result(880), 1_400);
        assertEquals(PitchTracker.State.STABLE, accepted.state);
        assertTrue(accepted.frequencyHz > 800);
    }

    @Test public void medianAndFilterLimitSmallOutliersWithoutStoppingResponse() {
        PitchTracker tracker = new PitchTracker();
        acquire(tracker, 440, 1_000);
        tracker.update(result(440), 1_200);
        tracker.update(result(450), 1_300);
        PitchTracker.Frame frame = tracker.update(result(440), 1_400);
        assertEquals(PitchTracker.State.STABLE, frame.state);
        assertTrue(frame.frequencyHz >= 439 && frame.frequencyHz <= 445);
    }

    @Test public void doesNotHoldAnOldNoteIndefinitelyWhenNewAudioIsContradictory() {
        PitchTracker tracker = new PitchTracker();
        acquire(tracker, 440, 1_000);
        tracker.update(result(880), 1_200);
        tracker.update(result(1_000), 1_300);
        tracker.update(result(880), 1_400);
        tracker.update(result(1_000), 1_500);
        tracker.update(result(880), 1_600);
        assertEquals(PitchTracker.State.NONE, tracker.update(result(1_000), 1_700).state);
    }
}

package org.jpi59.ethictuner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PitchTrackerTest {
    private static PitchDetector.Result result(double hz) {
        return new PitchDetector.Result(hz, .9, .1);
    }

    @Test public void holdsTheLastHonestReadingAcrossAShortDropout() {
        PitchTracker tracker = new PitchTracker();
        assertEquals(PitchTracker.State.STABLE, tracker.update(result(440), 1_000).state);
        PitchTracker.Frame held = tracker.update(null, 1_400);
        assertEquals(PitchTracker.State.HELD, held.state);
        assertEquals(440, held.frequencyHz, .01);
        assertEquals(PitchTracker.State.NONE, tracker.update(null, 1_600).state);
    }

    @Test public void requiresRepeatedEvidenceBeforeAcceptingALargeJump() {
        PitchTracker tracker = new PitchTracker();
        tracker.update(result(440), 1_000);
        assertEquals(PitchTracker.State.AMBIGUOUS, tracker.update(result(880), 1_100).state);
        assertEquals(PitchTracker.State.AMBIGUOUS, tracker.update(result(880), 1_200).state);
        PitchTracker.Frame accepted = tracker.update(result(880), 1_300);
        assertEquals(PitchTracker.State.STABLE, accepted.state);
        assertTrue(accepted.frequencyHz > 800);
    }

    @Test public void medianAndFilterLimitSmallOutliersWithoutStoppingResponse() {
        PitchTracker tracker = new PitchTracker();
        tracker.update(result(440), 1_000);
        tracker.update(result(440), 1_100);
        tracker.update(result(450), 1_200);
        PitchTracker.Frame frame = tracker.update(result(440), 1_300);
        assertEquals(PitchTracker.State.STABLE, frame.state);
        assertTrue(frame.frequencyHz >= 439 && frame.frequencyHz <= 445);
    }

    @Test public void doesNotHoldAnOldNoteIndefinitelyWhenNewAudioIsContradictory() {
        PitchTracker tracker = new PitchTracker();
        tracker.update(result(440), 1_000);
        tracker.update(result(880), 1_100);
        tracker.update(result(1_000), 1_200);
        tracker.update(result(880), 1_300);
        tracker.update(result(1_000), 1_400);
        tracker.update(result(880), 1_500);
        assertEquals(PitchTracker.State.NONE, tracker.update(result(1_000), 1_600).state);
    }
}

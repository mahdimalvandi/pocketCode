package dev.pocketcode.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class TrailingSilenceDetectorTest {
    private static byte[] pcm(short sample) {
        return new byte[]{(byte)(sample&0xff),(byte)((sample>>>8)&0xff)};
    }

    @Test public void initialSilenceDoesNotEndCommandBeforeSpeechStarts() {
        TrailingSilenceDetector detector=new TrailingSilenceDetector(1600);
        assertFalse(detector.shouldStop(pcm((short)0),2,0L));
        assertFalse(detector.shouldStop(pcm((short)0),2,20_000_000_000L));
    }

    @Test public void stopsAtConfiguredContinuousSilenceBoundary() {
        TrailingSilenceDetector detector=new TrailingSilenceDetector(1600);
        assertFalse(detector.shouldStop(pcm((short)1000),2,1_000_000_000L));
        assertFalse(detector.shouldStop(pcm((short)0),2,2_599_000_000L));
        assertTrue(detector.shouldStop(pcm((short)0),2,2_600_000_000L));
    }

    @Test public void renewedSpeechResetsTrailingSilenceClock() {
        TrailingSilenceDetector detector=new TrailingSilenceDetector(1600);
        assertFalse(detector.shouldStop(pcm((short)1000),2,1_000_000_000L));
        assertFalse(detector.shouldStop(pcm((short)0),2,2_000_000_000L));
        assertFalse(detector.shouldStop(pcm((short)1000),2,2_500_000_000L));
        assertFalse(detector.shouldStop(pcm((short)0),2,4_000_000_000L));
        assertTrue(detector.shouldStop(pcm((short)0),2,4_100_000_000L));
    }

    @Test public void subThresholdNoiseDoesNotStartSpeechTimer() {
        TrailingSilenceDetector detector=new TrailingSilenceDetector(1600);
        assertFalse(detector.shouldStop(pcm((short)379),2,1_000_000_000L));
        assertFalse(detector.shouldStop(pcm((short)0),2,10_000_000_000L));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsOutOfRangeSilenceInterval() {
        new TrailingSilenceDetector(499);
    }
}

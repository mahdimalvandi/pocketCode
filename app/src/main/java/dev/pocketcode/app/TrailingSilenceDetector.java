package dev.pocketcode.app;

/** Tracks speech onset and the required continuous trailing-silence interval. */
final class TrailingSilenceDetector {
    private final long silenceNanos;
    private boolean speechDetected;
    private long lastSpeechNanos;

    TrailingSilenceDetector(int silenceMillis) {
        if(silenceMillis<500 || silenceMillis>5000)
            throw new IllegalArgumentException("Invalid trailing-silence interval.");
        silenceNanos=silenceMillis*1_000_000L;
    }

    boolean shouldStop(byte[] pcm,int length,long nowNanos) {
        if(VoiceRecorder.hasSpeech(pcm,length)) {
            speechDetected=true;
            lastSpeechNanos=nowNanos;
            return false;
        }
        return speechDetected && nowNanos-lastSpeechNanos>=silenceNanos;
    }
}

package dev.pocketcode.app;

import org.junit.Test;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.Assert.*;

public class VoiceRecorderTest {
    private static byte[] pcm(short... samples) {
        ByteBuffer buffer=ByteBuffer.allocate(samples.length*2).order(ByteOrder.LITTLE_ENDIAN);
        for(short sample:samples)buffer.putShort(sample);
        return buffer.array();
    }

    @Test public void silenceHasZeroRms() {
        assertEquals(0,VoiceRecorder.calculateRms(pcm((short)0,(short)0),4));
        assertFalse(VoiceRecorder.hasSpeech(pcm((short)0,(short)0),4));
    }

    @Test public void rmsUsesSignedPcm16Samples() {
        assertEquals(400,VoiceRecorder.calculateRms(pcm((short)400,(short)-400),4));
    }

    @Test public void voiceActivityThresholdIsInclusiveAt380() {
        assertFalse(VoiceRecorder.hasSpeech(pcm((short)379,(short)-379),4));
        assertTrue(VoiceRecorder.hasSpeech(pcm((short)380,(short)-380),4));
    }

    @Test public void emptyPcmChunkIsNotSpeech() {
        assertEquals(0,VoiceRecorder.calculateRms(new byte[0],0));
        assertFalse(VoiceRecorder.hasSpeech(new byte[0],0));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidPcmRange() {
        VoiceRecorder.calculateRms(new byte[2],3);
    }

    @Test public void writesValidMono16KhzPcmWavHeader() {
        byte[] samples=pcm((short)1,(short)-1);
        byte[] wav=VoiceRecorder.encodeWav(samples);
        assertEquals(48,wav.length);
        assertEquals("RIFF",new String(wav,0,4,StandardCharsets.US_ASCII));
        assertEquals("WAVE",new String(wav,8,4,StandardCharsets.US_ASCII));
        assertEquals("fmt ",new String(wav,12,4,StandardCharsets.US_ASCII));
        assertEquals("data",new String(wav,36,4,StandardCharsets.US_ASCII));
        ByteBuffer header=ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(40,header.getInt(4));
        assertEquals(1,header.getShort(20));
        assertEquals(1,header.getShort(22));
        assertEquals(16000,header.getInt(24));
        assertEquals(32000,header.getInt(28));
        assertEquals(2,header.getShort(32));
        assertEquals(16,header.getShort(34));
        assertEquals(4,header.getInt(40));
        assertArrayEquals(samples,Arrays.copyOfRange(wav,44,wav.length));
    }

    @Test public void validatesRecorderWavBeforeSendingToRouter() {
        byte[] valid=VoiceRecorder.encodeWav(pcm((short)300,(short)-300));
        assertTrue(VoiceRecorder.isValidPcm16MonoWav(valid));

        byte[] wrongRate=Arrays.copyOf(valid,valid.length);
        ByteBuffer.wrap(wrongRate).order(ByteOrder.LITTLE_ENDIAN).putInt(24,8000);
        assertFalse(VoiceRecorder.isValidPcm16MonoWav(wrongRate));
    }

    @Test public void rejectsTruncatedAndOversizedWav() {
        assertFalse(VoiceRecorder.isValidPcm16MonoWav(new byte[43]));
        byte[] tooLarge=new byte[44+16000*2*60+2];
        assertFalse(VoiceRecorder.isValidPcm16MonoWav(tooLarge));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsOddLengthPcmForWav() {
        VoiceRecorder.encodeWav(new byte[]{1});
    }
}

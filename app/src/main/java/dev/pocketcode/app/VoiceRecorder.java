package dev.pocketcode.app;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** In-memory mono PCM recorder. The automatic mode ends after trailing silence. */
final class VoiceRecorder {
    private AudioRecord recorder;
    private Thread thread;
    private volatile boolean running;
    private volatile String failure;
    private final ByteArrayOutputStream pcm=new ByteArrayOutputStream();
    private int maximumSeconds=60;
    private int trailingSilenceMillis;
    private Runnable autoStopCallback;

    synchronized void start() throws Exception {
        startInternal(60,0,null);
    }

    synchronized void startUntilSilence(int maxSeconds,int silenceMillis,Runnable callback) throws Exception {
        if(maxSeconds<2 || maxSeconds>60 || silenceMillis<500 || silenceMillis>5000 || callback==null)
            throw new IllegalArgumentException("Invalid automatic recording limits.");
        startInternal(maxSeconds,silenceMillis,callback);
    }

    private void startInternal(int maxSeconds,int silenceMillis,Runnable callback) throws Exception {
        if(recorder!=null)throw new Exception("Recording already started.");
        pcm.reset();failure=null;
        maximumSeconds=maxSeconds;
        trailingSilenceMillis=silenceMillis;
        autoStopCallback=callback;
        int buffer=Math.max(4096,AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT));
        recorder=new AudioRecord(MediaRecorder.AudioSource.MIC,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,buffer);
        if(recorder.getState()!=AudioRecord.STATE_INITIALIZED){recorder.release();recorder=null;throw new Exception("Microphone unavailable.");}
        try{recorder.startRecording();}catch(Exception e){recorder.release();recorder=null;throw e;}
        running=true;
        thread=new Thread(()->{
            byte[] data=new byte[2048];
            boolean speechDetected=false;
            long lastSpeechNanos=System.nanoTime();
            int maximumBytes=16000*2*maximumSeconds;
            while(running&&pcm.size()<maximumBytes){
                int n=recorder.read(data,0,Math.min(data.length,maximumBytes-pcm.size()));
                if(n<0){failure="Microphone read failed.";break;}
                if(n>0){
                    if(trailingSilenceMillis>0){
                        long now=System.nanoTime();
                        if(hasSpeech(data,n)){speechDetected=true;lastSpeechNanos=now;}
                        else if(speechDetected && now-lastSpeechNanos>=trailingSilenceMillis*1_000_000L){
                            pcm.write(data,0,n);
                            break;
                        }
                    }
                    pcm.write(data,0,n);
                }
            }
            running=false;
            Runnable callbackToRun=autoStopCallback;
            if(callbackToRun!=null)callbackToRun.run();
        },"pocket-voice");
        thread.start();
    }

    synchronized byte[] stop(boolean discard) throws Exception {
        if(recorder==null)throw new Exception("No active recording.");
        running=false;
        try{
            recorder.stop();
            thread.join(2000);
            if(thread.isAlive())throw new Exception("Microphone did not stop.");
        } finally {
            recorder.release();recorder=null;
            autoStopCallback=null;
        }
        byte[] samples=pcm.toByteArray();pcm.reset();
        if(discard)return new byte[0];
        if(failure!=null)throw new Exception(failure);
        if(samples.length<3200)throw new Exception("Recording is too short.");
        return encodeWav(samples);
    }

    static int calculateRms(byte[] data,int length) {
        if(data==null || length<0 || length>data.length)
            throw new IllegalArgumentException("Invalid PCM sample range.");
        int samples=length/2;
        if(samples==0)return 0;
        long energy=0;
        for(int i=0;i<samples;i++){
            int sample=(short)((data[i*2]&0xff)|(data[i*2+1]<<8));
            energy+=(long)sample*sample;
        }
        return (int)Math.sqrt(energy/(double)samples);
    }

    static boolean hasSpeech(byte[] data,int length) {
        return calculateRms(data,length)>=380;
    }

    static byte[] encodeWav(byte[] samples) {
        if(samples==null || (samples.length&1)!=0)
            throw new IllegalArgumentException("PCM data must contain complete 16-bit samples.");
        ByteBuffer wav=ByteBuffer.allocate(44+samples.length).order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36+samples.length).put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII))
            .putInt(16).putShort((short)1).putShort((short)1).putInt(16000).putInt(32000).putShort((short)2).putShort((short)16)
            .put("data".getBytes(StandardCharsets.US_ASCII)).putInt(samples.length).put(samples);
        return wav.array();
    }
}

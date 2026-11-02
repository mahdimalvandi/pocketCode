package dev.pocketcode.app;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** In-memory mono PCM, maximum 60 seconds. No recording survives stop/cancel. */
final class VoiceRecorder {
    private AudioRecord recorder;
    private Thread thread;
    private volatile boolean running;
    private volatile String failure;
    private final ByteArrayOutputStream pcm=new ByteArrayOutputStream();
    synchronized void start() throws Exception {
        if(recorder!=null)throw new Exception("Recording already started.");
        int buffer=Math.max(4096,AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT));
        recorder=new AudioRecord(MediaRecorder.AudioSource.MIC,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,buffer);
        if(recorder.getState()!=AudioRecord.STATE_INITIALIZED){recorder.release();recorder=null;throw new Exception("Microphone unavailable.");}
        try{recorder.startRecording();}catch(Exception e){recorder.release();recorder=null;throw e;}
        running=true;
        thread=new Thread(()->{
            byte[] data=new byte[4096];
            while(running&&pcm.size()<16000*2*60){
                int n=recorder.read(data,0,Math.min(data.length,16000*2*60-pcm.size()));
                if(n<0){failure="Microphone read failed.";break;}
                if(n>0)pcm.write(data,0,n);
            }
            running=false;
        },"pocket-voice");thread.start();
    }
    synchronized byte[] stop(boolean discard) throws Exception {
        if(recorder==null)throw new Exception("No active recording.");
        running=false;
        try{recorder.stop();thread.join(2000);if(thread.isAlive())throw new Exception("Microphone did not stop.");}
        finally{recorder.release();recorder=null;}
        byte[] samples=pcm.toByteArray();pcm.reset();
        if(discard)return new byte[0];
        if(failure!=null)throw new Exception(failure);
        if(samples.length<3200)throw new Exception("Recording is too short.");
        ByteBuffer wav=ByteBuffer.allocate(44+samples.length).order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes("US-ASCII")).putInt(36+samples.length).put("WAVEfmt ".getBytes("US-ASCII"))
            .putInt(16).putShort((short)1).putShort((short)1).putInt(16000).putInt(32000).putShort((short)2).putShort((short)16)
            .put("data".getBytes("US-ASCII")).putInt(samples.length).put(samples);
        return wav.array();
    }
}

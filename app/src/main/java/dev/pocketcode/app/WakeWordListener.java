package dev.pocketcode.app;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/** On-device-only wake phrase detector. It is owned by MainActivity and runs only while visible. */
final class WakeWordListener implements RecognitionListener {
    interface Callback {
        void onWakeWord();
        void onUnavailable(String reason);
    }

    private static final int MAX_CONSECUTIVE_FAILURES=8;
    private final Context context;
    private final Callback callback;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private boolean running;
    private int consecutiveFailures;

    WakeWordListener(Context context,Callback callback) {
        this.context=context.getApplicationContext();
        this.callback=callback;
    }

    static boolean isAvailable(Context context) {
        return Build.VERSION.SDK_INT>=Build.VERSION_CODES.S
                && SpeechRecognizer.isOnDeviceRecognitionAvailable(context);
    }

    void start() {
        if(Looper.myLooper()!=Looper.getMainLooper())
            throw new IllegalStateException("SpeechRecognizer must be started on the main thread.");
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.S)
            throw new IllegalStateException("On-device speech recognition requires Android 12 or newer.");
        if(!SpeechRecognizer.isOnDeviceRecognitionAvailable(context))
            throw new IllegalStateException("On-device speech recognition is unavailable.");
        stop();
        consecutiveFailures=0;
        recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
        recognizer.setRecognitionListener(this);
        running=true;
        listen();
    }

    private void listen() {
        if(!running||recognizer==null)return;
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR")
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
        if(Build.VERSION.SDK_INT>=33) {
            ArrayList<String> bias=new ArrayList<>();
            bias.add("پاکت کد");
            bias.add("Pocket Code");
            bias.add("پاکت‌کد");
            intent.putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS,bias);
        }
        try {
            recognizer.startListening(intent);
        } catch(RuntimeException e) {
            scheduleFailure(1200);
        }
    }

    private void scheduleRetry(long delayMs) {
        if(!running)return;
        handler.removeCallbacks(retry);
        handler.postDelayed(retry,delayMs);
    }

    private void scheduleFailure(long baseDelayMs) {
        if(!running)return;
        consecutiveFailures++;
        if(consecutiveFailures>=MAX_CONSECUTIVE_FAILURES) {
            running=false;
            handler.removeCallbacksAndMessages(null);
            stopRecognizer();
            callback.onUnavailable("تشخیص آفلاین چند بار پشت سر هم ناموفق بود و برای جلوگیری از درگیری مداوم میکروفن متوقف شد. وضعیت سرویس گفتار و مجوز میکروفن را بررسی کن و بیدارباش را دوباره فعال کن.");
            return;
        }
        long backoff=500L<<Math.min(consecutiveFailures-1,3);
        scheduleRetry(Math.min(5000L,Math.max(baseDelayMs,backoff)));
    }

    private final Runnable retry=this::listen;

    @Override public void onResults(Bundle results) {
        if(!running)return;
        handler.removeCallbacks(retry);
        consecutiveFailures=0;
        ArrayList<String> matches=results==null?null:
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if(matches!=null) {
            for(String match:matches) {
                if(WakePhraseMatcher.matches(match)) {
                    running=false;
                    stopRecognizer();
                    callback.onWakeWord();
                    return;
                }
            }
        }
        scheduleRetry(350);
    }

    @Override public void onError(int error) {
        if(!running)return;
        handler.removeCallbacks(retry);
        if(error==SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
                ||error==SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) {
            running=false;
            handler.removeCallbacksAndMessages(null);
            stopRecognizer();
            callback.onUnavailable("مدل گفتار آفلاین فارسی روی دستگاه در دسترس نیست؛ زبان فارسی را در سرویس گفتار آفلاین اندروید نصب کن.");
            return;
        }
        // Empty results and speech timeouts are normal when listening for a wake phrase.
        if(error==SpeechRecognizer.ERROR_NO_MATCH||error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            consecutiveFailures=0;
            scheduleRetry(350);
            return;
        }
        scheduleFailure(error==SpeechRecognizer.ERROR_TOO_MANY_REQUESTS?2500:900);
    }

    void stop() {
        running=false;
        handler.removeCallbacksAndMessages(null);
        stopRecognizer();
    }

    private void stopRecognizer() {
        if(recognizer!=null) {
            try { recognizer.cancel(); } catch(RuntimeException ignored) { }
            try { recognizer.destroy(); } catch(RuntimeException ignored) { }
            recognizer=null;
        }
    }

    @Override public void onReadyForSpeech(Bundle params) { }
    @Override public void onBeginningOfSpeech() { }
    @Override public void onRmsChanged(float rmsdB) { }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { }
    @Override public void onPartialResults(Bundle partialResults) { }
    @Override public void onEvent(int eventType,Bundle params) { }
}

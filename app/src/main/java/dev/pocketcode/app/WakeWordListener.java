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
import java.util.Locale;

/** On-device-only wake phrase detector. It is owned by MainActivity and runs only while visible. */
final class WakeWordListener implements RecognitionListener {
    interface Callback {
        void onWakeWord();
        void onUnavailable(String reason);
    }

    private final Context context;
    private final Callback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private boolean running;

    WakeWordListener(Context context, Callback callback) {
        this.context=context.getApplicationContext();
        this.callback=callback;
    }

    static boolean isAvailable(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && SpeechRecognizer.isOnDeviceRecognitionAvailable(context);
    }

    void start() {
        if (Looper.myLooper()!=Looper.getMainLooper())
            throw new IllegalStateException("SpeechRecognizer must be started on the main thread.");
        if (!isAvailable(context))
            throw new IllegalStateException("On-device speech recognition is unavailable.");
        stop();
        recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
        recognizer.setRecognitionListener(this);
        running=true;
        listen();
    }

    private void listen() {
        if (!running || recognizer==null) return;
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR")
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
        if (Build.VERSION.SDK_INT >= 33) {
            ArrayList<String> bias=new ArrayList<>();
            bias.add("پاکت کد");
            bias.add("Pocket Code");
            bias.add("پاکت‌کد");
            intent.putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS,bias);
        }
        try {
            recognizer.startListening(intent);
        } catch (RuntimeException e) {
            scheduleRetry(1200);
        }
    }

    private void scheduleRetry(long delayMs) {
        if (!running) return;
        handler.removeCallbacks(retry);
        handler.postDelayed(retry,delayMs);
    }

    private final Runnable retry=this::listen;

    private static boolean matchesWakePhrase(String phrase) {
        if (phrase==null) return false;
        String normalized=phrase.toLowerCase(Locale.ROOT)
                .replace('ي','ی').replace('ك','ک')
                .replaceAll("[^\\p{L}\\p{N}]","");
        return normalized.contains("pocketcode")
                || normalized.contains("پاکتکد")
                || normalized.contains("پاکتکود");
    }

    @Override public void onResults(Bundle results) {
        if (!running) return;
        ArrayList<String> matches=results==null?null:
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches!=null) {
            for (String match:matches) {
                if (matchesWakePhrase(match)) {
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
        if (!running) return;
        if (error==SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
                || error==SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) {
            running=false;
            stopRecognizer();
            callback.onUnavailable("مدل گفتار آفلاین فارسی روی دستگاه در دسترس نیست؛ زبان فارسی را در سرویس گفتار آفلاین اندروید نصب کن.");
            return;
        }
        scheduleRetry(error==SpeechRecognizer.ERROR_TOO_MANY_REQUESTS?2500:900);
    }

    void stop() {
        running=false;
        handler.removeCallbacksAndMessages(null);
        stopRecognizer();
    }

    private void stopRecognizer() {
        if (recognizer!=null) {
            try { recognizer.cancel(); } catch (RuntimeException ignored) { }
            try { recognizer.destroy(); } catch (RuntimeException ignored) { }
            recognizer=null;
        }
    }

    @Override public void onReadyForSpeech(Bundle params) { }
    @Override public void onBeginningOfSpeech() { }
    @Override public void onRmsChanged(float rmsdB) { }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { }
    @Override public void onPartialResults(Bundle partialResults) { }
    @Override public void onEvent(int eventType, Bundle params) { }
}

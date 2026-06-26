package id.zacky.voicememo.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

public class SpeechToTextHelper {

    private SpeechRecognizer speechRecognizer;
    private final Intent speechRecognizerIntent;
    private final STTListener listener;
    
    private final android.os.Handler silenceHandler = new android.os.Handler();
    private final Runnable silenceRunnable = new Runnable() {
        @Override
        public void run() {
            stopListening();
        }
    };

    public interface STTListener {
        void onResult(String text);
        void onPartialResult(String partialText);
        void onError(String error);
    }

    public SpeechToTextHelper(Context context, STTListener listener) {
        this.listener = listener;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID"); 
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                silenceHandler.removeCallbacks(silenceRunnable);
            }

            @Override
            public void onError(int error) {
                silenceHandler.removeCallbacks(silenceRunnable);
                listener.onError("Error code: " + error);
            }

            @Override
            public void onResults(Bundle results) {
                silenceHandler.removeCallbacks(silenceRunnable);
                java.util.ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    listener.onResult(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // Reset silence timer
                silenceHandler.removeCallbacks(silenceRunnable);
                silenceHandler.postDelayed(silenceRunnable, 3000);
                
                java.util.ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    listener.onPartialResult(matches.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    public void startListening() {
        speechRecognizer.startListening(speechRecognizerIntent);
        silenceHandler.removeCallbacks(silenceRunnable);
        silenceHandler.postDelayed(silenceRunnable, 3000); // 3 seconds timeout
    }

    public void stopListening() {
        speechRecognizer.stopListening();
    }
    
    public void destroy() {
        silenceHandler.removeCallbacks(silenceRunnable);
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}

package id.zacky.voicememo.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public class ReminderReceiver extends BroadcastReceiver {
    
    private TextToSpeechHelper ttsHelper;

    @Override
    public void onReceive(Context context, Intent intent) {
        String noteTitle = intent.getStringExtra("NOTE_TITLE");
        String noteContent = intent.getStringExtra("NOTE_CONTENT");
        
        if (noteTitle != null) {
            NotificationHelper.showNotification(context, "Pengingat: " + noteTitle, noteContent);
            
            // Initialize TTS
            ttsHelper = new TextToSpeechHelper(context.getApplicationContext());
            
            // Delay to allow TTS engine initialization
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (noteContent != null && !noteContent.isEmpty()) {
                    ttsHelper.speak("Pengingat catatan: " + noteTitle + ". " + noteContent);
                } else {
                    ttsHelper.speak("Pengingat catatan: " + noteTitle);
                }
            }, 1000);
        }
    }
}

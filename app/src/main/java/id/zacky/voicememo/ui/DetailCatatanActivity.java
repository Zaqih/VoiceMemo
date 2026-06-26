package id.zacky.voicememo.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import id.zacky.voicememo.data.AppDatabase;
import id.zacky.voicememo.data.entity.Category;
import id.zacky.voicememo.data.entity.Note;
import id.zacky.voicememo.databinding.ActivityDetailCatatanBinding;
import id.zacky.voicememo.ui.dialog.KategoriDialogFragment;
import id.zacky.voicememo.utils.ReminderReceiver;
import id.zacky.voicememo.utils.SpeechToTextHelper;
import id.zacky.voicememo.utils.TextToSpeechHelper;
import id.zacky.voicememo.utils.NotificationHelper;

public class DetailCatatanActivity extends AppCompatActivity implements SpeechToTextHelper.STTListener {

    private ActivityDetailCatatanBinding binding;
    private AppDatabase db;
    private Note currentNote;
    
    private SpeechToTextHelper sttHelper;
    private TextToSpeechHelper ttsHelper;
    private boolean isListening = false;
    private boolean isSpeaking = false;
    
    private List<Category> categoryList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    
    private String textBeforeDictation = "";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailCatatanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        NotificationHelper.createNotificationChannel(this);

        db = AppDatabase.getDatabase(this);
        
        sttHelper = new SpeechToTextHelper(this, this);
        ttsHelper = new TextToSpeechHelper(this);

        setupSpinner();

        int noteId = getIntent().getIntExtra("NOTE_ID", -1);
        if (noteId != -1) {
            loadNote(noteId);
        } else {
            currentNote = new Note("", "", 0);
        }

        binding.btnMic.setOnClickListener(v -> {
            if (sttHelper == null) return;
            if (checkAudioPermission()) {
                if (isListening) {
                    sttHelper.stopListening();
                    isListening = false;
                    binding.btnMic.setColorFilter(null);
                } else {
                    textBeforeDictation = binding.etContent.getText().toString();
                    sttHelper.startListening();
                    isListening = true;
                    binding.btnMic.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_light));
                }
            }
        });

        binding.btnTTS.setOnClickListener(v -> {
            if (isSpeaking) {
                ttsHelper.stop();
                isSpeaking = false;
                binding.btnTTS.setColorFilter(null);
            } else {
                String text = binding.etContent.getText().toString();
                if (!text.isEmpty()) {
                    ttsHelper.speak(text);
                    isSpeaking = true;
                    binding.btnTTS.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_blue_light));
                }
            }
        });

        binding.btnReminder.setOnClickListener(v -> {
            if (checkNotificationPermission()) {
                showTimePicker();
            }
        });
        
        binding.btnShare.setOnClickListener(v -> shareNote());
        
        binding.btnAddCategory.setOnClickListener(v -> {
            KategoriDialogFragment dialog = new KategoriDialogFragment(categoryName -> {
                saveCategory(categoryName);
            });
            dialog.show(getSupportFragmentManager(), "KategoriDialog");
        });
        
        binding.btnDelete.setOnClickListener(v -> deleteNote());
    }

    private void setupSpinner() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(spinnerAdapter);
        loadCategories();
    }

    private void loadCategories() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryList = db.noteDao().getAllCategories();
            List<String> categoryNames = new ArrayList<>();
            categoryNames.add("Tanpa Kategori"); // Default id 0
            
            for (Category c : categoryList) {
                categoryNames.add(c.name);
            }
            
            runOnUiThread(() -> {
                spinnerAdapter.clear();
                spinnerAdapter.addAll(categoryNames);
                spinnerAdapter.notifyDataSetChanged();
                
                // Select proper category
                if (currentNote != null && currentNote.categoryId > 0) {
                    for (int i = 0; i < categoryList.size(); i++) {
                        if (categoryList.get(i).id == currentNote.categoryId) {
                            binding.spinnerCategory.setSelection(i + 1);
                            break;
                        }
                    }
                }
            });
        });
    }

    private void saveCategory(String name) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long newId = db.noteDao().insertCategory(new Category(name));
            if (currentNote != null) {
                currentNote.categoryId = (int) newId;
            }
            loadCategories();
        });
    }

    private void loadNote(int noteId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            currentNote = db.noteDao().getNoteById(noteId);
            runOnUiThread(() -> {
                if (currentNote != null) {
                    binding.etTitle.setText(currentNote.title);
                    binding.etContent.setText(currentNote.content);
                    loadCategories(); // Reload to set correct selection
                }
            });
        });
    }

    private void saveNote() {
        if (currentNote == null) return;
        
        currentNote.title = binding.etTitle.getText().toString();
        currentNote.content = binding.etContent.getText().toString();
        
        int spinnerPos = binding.spinnerCategory.getSelectedItemPosition();
        if (spinnerPos > 0 && spinnerPos - 1 < categoryList.size()) {
            currentNote.categoryId = categoryList.get(spinnerPos - 1).id;
        } else {
            currentNote.categoryId = 0;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (currentNote.id == 0) {
                // If id is 0, we need to get the inserted id to avoid re-inserting if paused/resumed again before close
                int id = (int) ((id.zacky.voicememo.data.dao.NoteDao) db.noteDao()).insertNoteAndReturnId(currentNote);
                currentNote.id = id;
            } else {
                db.noteDao().updateNote(currentNote);
            }
        });
    }

    private void shareNote() {
        String text = binding.etTitle.getText().toString() + "\n\n" + binding.etContent.getText().toString();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Bagikan Catatan"));
    }

    private void deleteNote() {
        if (currentNote != null && currentNote.id != 0) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.noteDao().deleteNote(currentNote);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Catatan dihapus", Toast.LENGTH_SHORT).show();
                    currentNote = null; // Set to null to prevent auto-save in onPause
                    finish();
                });
            });
        } else {
            currentNote = null;
            finish();
        }
    }

    private void showTimePicker() {
        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            Calendar reminderTime = Calendar.getInstance();
            reminderTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            reminderTime.set(Calendar.MINUTE, minute1);
            reminderTime.set(Calendar.SECOND, 0);

            if (reminderTime.before(Calendar.getInstance())) {
                reminderTime.add(Calendar.DATE, 1);
            }
            
            setReminder(reminderTime.getTimeInMillis());
        }, hour, minute, true);
        timePickerDialog.show();
    }

    private void setReminder(long timeInMillis) {
        saveNote(); // Make sure note is saved before setting reminder
        
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("NOTE_TITLE", binding.etTitle.getText().toString());
        intent.putExtra("NOTE_CONTENT", binding.etContent.getText().toString());

        // Use a unique request code based on note ID
        int reqCode = currentNote.id > 0 ? currentNote.id : (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            }
            Toast.makeText(this, "Pengingat disetel", Toast.LENGTH_SHORT).show();
            
            currentNote.reminderTime = timeInMillis;
            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.noteDao().updateNote(currentNote);
            });
        }
    }

    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return false;
        }
        return true;
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 201);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (sttHelper != null) {
                    textBeforeDictation = binding.etContent.getText().toString();
                    sttHelper.startListening();
                    isListening = true;
                    binding.btnMic.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_light));
                }
            } else {
                Toast.makeText(this, "Izin mikrofon ditolak", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 201) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showTimePicker();
            } else {
                Toast.makeText(this, "Izin notifikasi diperlukan untuk pengingat", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResult(String text) {
        isListening = false;
        binding.btnMic.setColorFilter(null);
        
        if (id.zacky.voicememo.utils.VoiceCommandParser.isAlarmCommand(text)) {
            Calendar time = id.zacky.voicememo.utils.VoiceCommandParser.parseTime(text);
            if (time != null) {
                String cleanContent = id.zacky.voicememo.utils.VoiceCommandParser.extractNoteContent(text);
                binding.etContent.setText(textBeforeDictation + (textBeforeDictation.isEmpty() ? "" : " ") + cleanContent);
                
                setReminder(time.getTimeInMillis());
                
                String reply = "Baik, pengingat disetel untuk jam " + time.get(Calendar.HOUR_OF_DAY) + " lebih " + time.get(Calendar.MINUTE);
                if (ttsHelper != null) {
                    ttsHelper.speak(reply);
                }
                return;
            }
        }
        
        if (textBeforeDictation.isEmpty()) {
            binding.etContent.setText(text);
        } else {
            binding.etContent.setText(textBeforeDictation + " " + text);
        }
    }

    @Override
    public void onPartialResult(String partialText) {
        if (textBeforeDictation.isEmpty()) {
            binding.etContent.setText(partialText);
        } else {
            binding.etContent.setText(textBeforeDictation + " " + partialText);
        }
    }

    @Override
    public void onError(String error) {
        isListening = false;
        binding.btnMic.setColorFilter(null);
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentNote != null) {
            saveNote(); // Auto save
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sttHelper.destroy();
        ttsHelper.destroy();
    }
}

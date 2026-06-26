package id.zacky.voicememo.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import id.zacky.voicememo.data.AppDatabase;
import id.zacky.voicememo.data.entity.Note;
import id.zacky.voicememo.databinding.FragmentReminderBinding;
import id.zacky.voicememo.ui.DetailCatatanActivity;
import id.zacky.voicememo.ui.adapter.NoteAdapter;

public class ReminderFragment extends Fragment {

    private FragmentReminderBinding binding;
    private NoteAdapter adapter;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReminderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getDatabase(requireContext());

        adapter = new NoteAdapter(note -> {
            Intent intent = new Intent(requireActivity(), DetailCatatanActivity.class);
            intent.putExtra("NOTE_ID", note.id);
            startActivity(intent);
        });

        binding.rvReminders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReminders.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReminders();
    }

    private void loadReminders() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Note> notes = db.noteDao().getNotesWithReminders();
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                adapter.setNotes(notes);
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

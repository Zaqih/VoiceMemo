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

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import id.zacky.voicememo.data.AppDatabase;
import id.zacky.voicememo.data.entity.Category;
import id.zacky.voicememo.data.entity.Note;
import id.zacky.voicememo.databinding.FragmentHomeBinding;
import id.zacky.voicememo.ui.DetailCatatanActivity;
import id.zacky.voicememo.ui.adapter.NoteAdapter;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private NoteAdapter adapter;
    private List<Category> categoryList = new ArrayList<>();
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
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

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        binding.fabAddNote.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), DetailCatatanActivity.class));
        });

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadNotesForTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCategoriesAndNotes();
    }

    private void loadCategoriesAndNotes() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryList = db.noteDao().getAllCategories();

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                int currentTab = binding.tabLayout.getSelectedTabPosition();
                binding.tabLayout.removeAllTabs();
                binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Semua"));

                for (Category category : categoryList) {
                    binding.tabLayout.addTab(binding.tabLayout.newTab().setText(category.name));
                }

                if (currentTab >= 0 && currentTab < binding.tabLayout.getTabCount()) {
                    binding.tabLayout.getTabAt(currentTab).select();
                } else {
                    loadNotesForTab(0);
                }
            });
        });
    }

    private void loadNotesForTab(int position) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Note> notes;
            if (position <= 0) {
                notes = db.noteDao().getAllNotes();
            } else {
                int categoryId = categoryList.get(position - 1).id;
                notes = db.noteDao().getNotesByCategory(categoryId);
            }

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

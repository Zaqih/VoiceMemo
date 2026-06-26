package id.zacky.voicememo.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import id.zacky.voicememo.databinding.DialogKategoriBinding;

public class KategoriDialogFragment extends DialogFragment {

    private final OnCategoryAddedListener listener;

    public interface OnCategoryAddedListener {
        void onCategoryAdded(String categoryName);
    }

    public KategoriDialogFragment(OnCategoryAddedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        DialogKategoriBinding binding = DialogKategoriBinding.inflate(LayoutInflater.from(getContext()));

        return new AlertDialog.Builder(requireContext())
                .setTitle("Tambah Kategori Baru")
                .setView(binding.getRoot())
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String name = binding.etCategoryName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        listener.onCategoryAdded(name);
                    }
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .create();
    }
}

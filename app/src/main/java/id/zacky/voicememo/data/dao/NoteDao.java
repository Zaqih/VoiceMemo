package id.zacky.voicememo.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import id.zacky.voicememo.data.entity.Category;
import id.zacky.voicememo.data.entity.Note;

@Dao
public interface NoteDao {
    @Insert
    void insertNote(Note note);
    
    @Insert
    long insertNoteAndReturnId(Note note);
    
    @Update
    void updateNote(Note note);
    
    @Delete
    void deleteNote(Note note);
    
    @Query("SELECT * FROM notes ORDER BY id DESC")
    List<Note> getAllNotes();
    
    @Query("SELECT * FROM notes WHERE categoryId = :categoryId ORDER BY id DESC")
    List<Note> getNotesByCategory(int categoryId);
    
    @Query("SELECT * FROM notes WHERE id = :noteId")
    Note getNoteById(int noteId);
    
    @Query("SELECT * FROM notes WHERE reminderTime > 0 ORDER BY reminderTime ASC")
    List<Note> getNotesWithReminders();
    
    @Insert
    long insertCategory(Category category);
    
    @Query("SELECT * FROM categories")
    List<Category> getAllCategories();
}

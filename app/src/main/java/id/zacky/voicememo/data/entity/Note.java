package id.zacky.voicememo.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String title;
    public String content;
    public int categoryId;
    public long reminderTime;
    
    public Note(String title, String content, int categoryId) {
        this.title = title;
        this.content = content;
        this.categoryId = categoryId;
        this.reminderTime = 0;
    }
}

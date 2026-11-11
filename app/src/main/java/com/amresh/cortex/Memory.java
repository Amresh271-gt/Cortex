package com.amresh.cortex;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** A saved fact or piece of info. Unlike a Task it has no time and never sends a reminder. */
@Entity(tableName = "memories")
public class Memory {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;

    /** The info itself, e.g. the Wi-Fi password. May be empty. */
    public String content;

    /** Optional category such as "Home" or "Work". May be null or empty. */
    public String tag;

    public boolean pinned;

    /** Epoch millis. */
    public long createdAt;

    /** Epoch millis. Used to sort, newest first. */
    public long updatedAt;
}

package com.amresh.cortex;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;

    /** Due time in epoch millis, or 0 when no time was found. */
    public long dueMillis;

    public boolean done;

    /** Time the task was moved to the recycle bin (epoch millis), or 0 while it is active. */
    @ColumnInfo(defaultValue = "0")
    public long deletedAt;

    /** Optional free-text notes for the task. */
    public String notes;
}

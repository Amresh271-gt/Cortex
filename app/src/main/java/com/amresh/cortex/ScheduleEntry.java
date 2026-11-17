package com.amresh.cortex;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * One line in a day's schedule. Entries made from a task keep the task's id in {@link #taskId};
 * entries the user adds by hand in the scheduler have taskId = 0.
 */
@Entity(tableName = "schedule_entries")
public class ScheduleEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Midnight (local time, epoch millis) of the day this entry belongs to. */
    public long dayStart;

    /** Time of the entry in epoch millis, or 0 for "any time" that day. */
    public long timeMillis;

    public String title;

    /** Id of the task this entry came from, or 0 for an entry added by hand. */
    public long taskId;

    /**
     * Set when the user removes a task entry from the schedule. The row is kept so the
     * automatic sync does not add the same task back again. The task itself is never touched.
     */
    public boolean hidden;

    /** Mirrors the task's done state for task entries. */
    public boolean done;

    /** Time the day passed and the entry moved to the schedule bin, or 0 while it is active. */
    public long archivedAt;
}

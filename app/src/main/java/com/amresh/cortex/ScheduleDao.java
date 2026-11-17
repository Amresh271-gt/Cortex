package com.amresh.cortex;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ScheduleDao {
    @Insert
    long insert(ScheduleEntry entry);

    @Update
    void update(ScheduleEntry entry);

    @Delete
    void delete(ScheduleEntry entry);

    /** Visible entries of one day, timed ones first in time order, "any time" ones last. */
    @Query("SELECT * FROM schedule_entries WHERE archivedAt = 0 AND hidden = 0 AND dayStart = :dayStart "
            + "ORDER BY CASE WHEN timeMillis = 0 THEN 1 ELSE 0 END, timeMillis ASC, id ASC")
    LiveData<List<ScheduleEntry>> observeDay(long dayStart);

    /** Schedule bin: entries of past days, newest day first. */
    @Query("SELECT * FROM schedule_entries WHERE archivedAt > 0 AND hidden = 0 "
            + "ORDER BY dayStart DESC, CASE WHEN timeMillis = 0 THEN 1 ELSE 0 END, timeMillis ASC, id ASC")
    LiveData<List<ScheduleEntry>> observeArchived();

    /** Active entries that came from a task (including the ones the user removed from view). */
    @Query("SELECT * FROM schedule_entries WHERE archivedAt = 0 AND taskId > 0")
    List<ScheduleEntry> getActiveTaskEntries();

    /** Moves every entry of a day before today into the schedule bin. */
    @Query("UPDATE schedule_entries SET archivedAt = :now WHERE archivedAt = 0 AND dayStart < :todayStart")
    int archiveBefore(long todayStart, long now);

    /** Permanently removes bin entries that were archived before the cutoff time. */
    @Query("DELETE FROM schedule_entries WHERE archivedAt > 0 AND archivedAt < :cutoff")
    int purgeArchivedBefore(long cutoff);

    @Query("DELETE FROM schedule_entries WHERE id = :id AND archivedAt > 0")
    void deleteArchivedById(long id);

    @Query("DELETE FROM schedule_entries WHERE archivedAt > 0")
    int deleteAllArchived();
}

package com.amresh.cortex;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {
    @Insert
    long insert(Task task);

    @Update
    void update(Task task);

    /** Active tasks (not in the recycle bin). */
    @Query("SELECT * FROM tasks WHERE deletedAt = 0 ORDER BY done ASC, CASE WHEN dueMillis = 0 THEN 1 ELSE 0 END, dueMillis ASC, id DESC")
    LiveData<List<Task>> observeActive();

    /** Recycle bin, most recently deleted first. */
    @Query("SELECT * FROM tasks WHERE deletedAt > 0 ORDER BY deletedAt DESC")
    LiveData<List<Task>> observeBin();

    /** Everything (active and bin), used for backups and de-duplication on restore. */
    @Query("SELECT * FROM tasks")
    List<Task> getAll();

    /** One task by id, or null if it no longer exists. Used when an alarm fires. */
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    Task getById(long id);

    @Query("SELECT * FROM tasks WHERE done = 0 AND dueMillis > 0 AND deletedAt = 0")
    List<Task> getPendingWithTime();

    /** Permanently removes bin items deleted before the cutoff time. */
    @Query("DELETE FROM tasks WHERE deletedAt > 0 AND deletedAt < :cutoff")
    int purgeBinOlderThan(long cutoff);
}

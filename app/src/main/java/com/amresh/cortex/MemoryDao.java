package com.amresh.cortex;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MemoryDao {
    @Insert
    long insert(Memory memory);

    @Update
    void update(Memory memory);

    @Delete
    void delete(Memory memory);

    /** Pinned memories first, then most recently changed. */
    @Query("SELECT * FROM memories ORDER BY pinned DESC, updatedAt DESC, id DESC")
    LiveData<List<Memory>> observeAll();

    /** Used for backups and de-duplication on restore. */
    @Query("SELECT * FROM memories")
    List<Memory> getAll();
}

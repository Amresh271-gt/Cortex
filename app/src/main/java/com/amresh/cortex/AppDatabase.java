package com.amresh.cortex;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cortex starts with its own fresh database (version 1). Data from the old Task Reminder app
 * is brought over with Restore / import, so no old migrations are needed here.
 */
@Database(entities = {Task.class, Memory.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /** How long deleted tasks stay in the recycle bin: 15 days. */
    public static final long BIN_RETENTION_MILLIS = 15L * 24 * 60 * 60 * 1000;

    public abstract TaskDao taskDao();

    public abstract MemoryDao memoryDao();

    /** Single background thread for all database writes. */
    public static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private static volatile AppDatabase instance;

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "cortex.db")
                            .build();
                }
            }
        }
        return instance;
    }
}

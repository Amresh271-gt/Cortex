package com.amresh.cortex;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cortex database. Version 1 had tasks and memories; version 2 adds the schedule table.
 * The migration only creates the new table, so existing tasks and memories are untouched.
 */
@Database(entities = {Task.class, Memory.class, ScheduleEntry.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /** How long deleted tasks stay in the recycle bin: 15 days. */
    public static final long BIN_RETENTION_MILLIS = 15L * 24 * 60 * 60 * 1000;

    public abstract TaskDao taskDao();

    public abstract MemoryDao memoryDao();

    public abstract ScheduleDao scheduleDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `schedule_entries` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`dayStart` INTEGER NOT NULL, "
                    + "`timeMillis` INTEGER NOT NULL, "
                    + "`title` TEXT, "
                    + "`taskId` INTEGER NOT NULL, "
                    + "`hidden` INTEGER NOT NULL, "
                    + "`done` INTEGER NOT NULL, "
                    + "`archivedAt` INTEGER NOT NULL)");
        }
    };

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
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}

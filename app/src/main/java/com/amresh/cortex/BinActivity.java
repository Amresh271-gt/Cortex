package com.amresh.cortex;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.List;

/**
 * Recycle bin with two lists: deleted tasks and past schedules. Both are kept for 15 days and
 * removed automatically, and every item (or the whole list) can also be deleted for good.
 */
public class BinActivity extends AppCompatActivity {

    private AppDatabase db;
    private RecyclerView rv;
    private TextView tvEmpty, tvHeader;
    private Button btnDeleteAll;
    private BinAdapter taskAdapter;
    private BinScheduleAdapter scheduleAdapter;

    private List<Task> tasks;
    private List<ScheduleEntry> schedules;
    private boolean showingTasks = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bin);
        setTitle(R.string.bin_title);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = AppDatabase.get(this);
        final long now = System.currentTimeMillis();
        AppDatabase.IO.execute(() -> {
            db.taskDao().purgeBinOlderThan(now - AppDatabase.BIN_RETENTION_MILLIS);
            db.scheduleDao().purgeArchivedBefore(now - ScheduleManager.BIN_RETENTION_MILLIS);
        });

        tvEmpty = findViewById(R.id.tvBinEmpty);
        tvHeader = findViewById(R.id.tvBinHeader);
        btnDeleteAll = findViewById(R.id.btnBinDeleteAll);
        rv = findViewById(R.id.rvBin);
        rv.setLayoutManager(new LinearLayoutManager(this));

        taskAdapter = new BinAdapter(this::confirmDeleteTask);
        scheduleAdapter = new BinScheduleAdapter(this::confirmDeleteEntry);

        MaterialButtonToggleGroup toggle = findViewById(R.id.binToggle);
        toggle.check(R.id.btnBinTasks);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            showingTasks = checkedId == R.id.btnBinTasks;
            render();
        });

        db.taskDao().observeBin().observe(this, list -> {
            tasks = list;
            taskAdapter.setTasks(list);
            render();
        });
        db.scheduleDao().observeArchived().observe(this, list -> {
            schedules = list;
            scheduleAdapter.setEntries(list);
            render();
        });

        btnDeleteAll.setOnClickListener(v -> confirmDeleteAll());
    }

    /** Shows the list for the selected tab and updates the text above it. */
    private void render() {
        if (showingTasks) {
            if (rv.getAdapter() != taskAdapter) rv.setAdapter(taskAdapter);
            tvHeader.setText(R.string.bin_info);
            tvEmpty.setText(R.string.bin_empty);
            setEmpty(tasks == null || tasks.isEmpty());
        } else {
            if (rv.getAdapter() != scheduleAdapter) rv.setAdapter(scheduleAdapter);
            tvHeader.setText(R.string.bin_info_schedules);
            tvEmpty.setText(R.string.bin_empty_schedules);
            setEmpty(schedules == null || schedules.isEmpty());
        }
    }

    private void setEmpty(boolean empty) {
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        btnDeleteAll.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // --------------------------------------------------------------- delete

    private void confirmDeleteTask(final Task task) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.bin_delete_title)
                .setMessage(R.string.bin_delete_msg)
                .setPositiveButton(R.string.bin_delete_yes,
                        (d, w) -> AppDatabase.IO.execute(() -> db.taskDao().deleteFromBin(task.id)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDeleteEntry(final ScheduleEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.bin_delete_title)
                .setMessage(R.string.bin_delete_msg)
                .setPositiveButton(R.string.bin_delete_yes,
                        (d, w) -> AppDatabase.IO.execute(() -> db.scheduleDao().deleteArchivedById(entry.id)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDeleteAll() {
        final boolean forTasks = showingTasks;
        new AlertDialog.Builder(this)
                .setTitle(forTasks ? R.string.bin_delete_all_tasks_title : R.string.bin_delete_all_schedules_title)
                .setMessage(R.string.bin_delete_msg)
                .setPositiveButton(R.string.bin_delete_yes, (d, w) -> AppDatabase.IO.execute(() -> {
                    if (forTasks) {
                        db.taskDao().deleteAllFromBin();
                    } else {
                        db.scheduleDao().deleteAllArchived();
                    }
                }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

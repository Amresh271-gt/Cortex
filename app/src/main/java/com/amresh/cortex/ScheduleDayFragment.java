package com.amresh.cortex;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;

/** One day of the schedule as a simple list, with a button to add an entry by hand. */
public class ScheduleDayFragment extends Fragment implements ScheduleAdapter.Listener {

    private static final String ARG_DAY = "day_start";

    private long dayStart;
    private AppDatabase db;

    public static ScheduleDayFragment newInstance(long dayStart) {
        ScheduleDayFragment f = new ScheduleDayFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_DAY, dayStart);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_day, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dayStart = requireArguments().getLong(ARG_DAY);
        db = AppDatabase.get(requireContext());

        TextView tvDate = view.findViewById(R.id.tvScheduleDate);
        final TextView tvEmpty = view.findViewById(R.id.tvScheduleEmpty);
        tvDate.setText(DateFormat.format("EEEE, d MMMM yyyy", dayStart));

        RecyclerView rv = view.findViewById(R.id.rvSchedule);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        final ScheduleAdapter adapter = new ScheduleAdapter(this);
        rv.setAdapter(adapter);

        db.scheduleDao().observeDay(dayStart).observe(getViewLifecycleOwner(), entries -> {
            adapter.setEntries(entries);
            tvEmpty.setVisibility(entries == null || entries.isEmpty() ? View.VISIBLE : View.GONE);
        });

        view.findViewById(R.id.btnScheduleAdd).setOnClickListener(v -> showAddDialog());
    }

    // ------------------------------------------------------------------ add

    private void showAddDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_schedule, null);
        final EditText etTitle = view.findViewById(R.id.etSchedTitle);
        final TextView tvTime = view.findViewById(R.id.tvSchedTime);
        Button btnPick = view.findViewById(R.id.btnSchedPickTime);
        Button btnClear = view.findViewById(R.id.btnSchedClearTime);

        final long[] time = {0};
        showTime(tvTime, time[0]);

        btnPick.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            if (time[0] > 0) c.setTimeInMillis(time[0]);
            new TimePickerDialog(requireContext(), (tp, hour, minute) -> {
                Calendar picked = Calendar.getInstance();
                picked.setTimeInMillis(dayStart);
                picked.set(Calendar.HOUR_OF_DAY, hour);
                picked.set(Calendar.MINUTE, minute);
                picked.set(Calendar.SECOND, 0);
                picked.set(Calendar.MILLISECOND, 0);
                time[0] = picked.getTimeInMillis();
                showTime(tvTime, time[0]);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(requireContext())).show();
        });
        btnClear.setOnClickListener(v -> {
            time[0] = 0;
            showTime(tvTime, 0);
        });

        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.schedule_add_title)
                .setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();

        // Set here so an empty title keeps the dialog open.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), R.string.empty_task, Toast.LENGTH_SHORT).show();
                return;
            }
            final ScheduleEntry e = new ScheduleEntry();
            e.dayStart = dayStart;
            e.timeMillis = time[0];
            e.title = title;
            e.taskId = 0; // added by hand, not in the task list
            AppDatabase.IO.execute(() -> db.scheduleDao().insert(e));
            dialog.dismiss();
        });
    }

    private void showTime(TextView tv, long millis) {
        if (millis > 0) {
            tv.setText(DateFormat.format("h:mm a", millis));
        } else {
            tv.setText(R.string.schedule_any_time);
        }
    }

    // --------------------------------------------------------------- remove

    @Override
    public void onRemove(final ScheduleEntry entry) {
        final Context app = requireContext().getApplicationContext();
        if (entry.taskId > 0) {
            // Task entry: hide it from the schedule only. The task stays in the task list.
            entry.hidden = true;
            AppDatabase.IO.execute(() -> db.scheduleDao().update(entry));
            Toast.makeText(app, R.string.schedule_removed_task, Toast.LENGTH_SHORT).show();
        } else {
            AppDatabase.IO.execute(() -> db.scheduleDao().delete(entry));
            Toast.makeText(app, R.string.schedule_removed, Toast.LENGTH_SHORT).show();
        }
    }
}

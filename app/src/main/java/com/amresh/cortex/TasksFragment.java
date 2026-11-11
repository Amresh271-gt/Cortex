package com.amresh.cortex;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Left tab: the task list with counters, typed and voice task entry, edit and delete. */
public class TasksFragment extends Fragment implements TaskAdapter.Listener {

    private AppDatabase db;
    private TaskAdapter adapter;
    private EditText etTask;
    private TextView tvTotal, tvDone, tvLeft;

    /** The mic on this tab always makes a task (no routing needed here). */
    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    ArrayList<String> texts =
                            result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (texts != null && !texts.isEmpty()) {
                        addTask(texts.get(0)); // spoken tasks are saved straight away
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.get(requireContext());

        etTask = view.findViewById(R.id.etTask);
        tvTotal = view.findViewById(R.id.tvTotal);
        tvDone = view.findViewById(R.id.tvDone);
        tvLeft = view.findViewById(R.id.tvLeft);
        Button btnAdd = view.findViewById(R.id.btnAdd);
        Button btnMic = view.findViewById(R.id.btnMic);

        RecyclerView rv = view.findViewById(R.id.rvTasks);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TaskAdapter(this);
        rv.setAdapter(adapter);

        db.taskDao().observeActive().observe(getViewLifecycleOwner(), tasks -> {
            adapter.setTasks(tasks);
            updateCounters(tasks);
        });

        btnAdd.setOnClickListener(v -> {
            String text = etTask.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), R.string.empty_task, Toast.LENGTH_SHORT).show();
                return;
            }
            addTask(text);
            etTask.setText("");
        });
        btnMic.setOnClickListener(v -> startVoiceInput());
    }

    private void startVoiceInput() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_prompt));
        try {
            speechLauncher.launch(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.no_speech, Toast.LENGTH_LONG).show();
        }
    }

    // ----------------------------------------------------------------- tasks

    private void addTask(String rawText) {
        final Context app = requireContext().getApplicationContext();
        TimeParser.Result r = TimeParser.parse(rawText);
        final Task t = new Task();
        t.title = r.title;
        t.dueMillis = r.dueMillis;
        t.done = false;
        AppDatabase.IO.execute(() -> {
            t.id = db.taskDao().insert(t);
            AlarmScheduler.schedule(app, t);
        });
    }

    private void updateCounters(List<Task> tasks) {
        int total = tasks == null ? 0 : tasks.size();
        int done = 0;
        if (tasks != null) {
            for (Task t : tasks) if (t.done) done++;
        }
        tvTotal.setText("Total: " + total);
        tvDone.setText("Done: " + done);
        tvLeft.setText("Left: " + (total - done));
    }

    @Override
    public void onToggle(final Task task, final boolean done) {
        final Context app = requireContext().getApplicationContext();
        task.done = done;
        AppDatabase.IO.execute(() -> {
            db.taskDao().update(task);
            if (done) {
                AlarmScheduler.cancel(app, task); // cancels the first and the second reminder
            } else {
                AlarmScheduler.schedule(app, task);
            }
        });
    }

    @Override
    public void onEdit(final Task task) {
        final Context app = requireContext().getApplicationContext();
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_task, null);
        final EditText etTitle = view.findViewById(R.id.etEditTitle);
        final EditText etNotes = view.findViewById(R.id.etEditNotes);
        final TextView tvDue = view.findViewById(R.id.tvEditDue);
        Button btnPick = view.findViewById(R.id.btnPickTime);
        Button btnClear = view.findViewById(R.id.btnClearTime);

        etTitle.setText(task.title);
        etTitle.setSelection(etTitle.getText().length());
        etNotes.setText(task.notes == null ? "" : task.notes);

        final long[] due = {task.dueMillis};
        showDue(tvDue, due[0]);

        btnPick.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            if (due[0] > 0) c.setTimeInMillis(due[0]);
            new DatePickerDialog(requireContext(), (dp, year, month, day) ->
                    new TimePickerDialog(requireContext(), (tp, hour, minute) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.set(year, month, day, hour, minute, 0);
                        picked.set(Calendar.MILLISECOND, 0);
                        due[0] = picked.getTimeInMillis();
                        showDue(tvDue, due[0]);
                    }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                            DateFormat.is24HourFormat(requireContext())).show(),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        btnClear.setOnClickListener(v -> {
            due[0] = 0;
            showDue(tvDue, 0);
        });

        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_task)
                .setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();

        // Set here so an empty title keeps the dialog open instead of closing it.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newTitle = etTitle.getText().toString().trim();
            if (newTitle.isEmpty()) {
                Toast.makeText(requireContext(), R.string.empty_task, Toast.LENGTH_SHORT).show();
                return;
            }
            String newNotes = etNotes.getText().toString().trim();
            task.title = newTitle;
            task.notes = newNotes.isEmpty() ? null : newNotes;
            task.dueMillis = due[0];
            AppDatabase.IO.execute(() -> {
                AlarmScheduler.cancel(app, task);
                db.taskDao().update(task);
                AlarmScheduler.schedule(app, task);
            });
            dialog.dismiss();
        });
    }

    private void showDue(TextView tv, long millis) {
        if (millis > 0) {
            tv.setText(DateFormat.format("EEE, d MMM yyyy, h:mm a", millis));
        } else {
            tv.setText(R.string.no_time);
        }
    }

    @Override
    public void onDelete(final Task task) {
        final Context app = requireContext().getApplicationContext();
        // Deleting moves the task to the recycle bin; it is removed for good after 15 days.
        task.deletedAt = System.currentTimeMillis();
        Toast.makeText(requireContext(), R.string.moved_to_bin, Toast.LENGTH_SHORT).show();
        AppDatabase.IO.execute(() -> {
            AlarmScheduler.cancel(app, task);
            db.taskDao().update(task);
        });
    }
}

package com.amresh.cortex;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.Holder> {

    public interface Listener {
        void onToggle(Task task, boolean done);

        void onEdit(Task task);

        void onDelete(Task task);
    }

    private final List<Task> items = new ArrayList<>();
    private final Listener listener;

    public TaskAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setTasks(List<Task> tasks) {
        items.clear();
        if (tasks != null) items.addAll(tasks);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final Task t = items.get(position);
        h.title.setText(t.title);
        h.title.setPaintFlags(t.done
                ? h.title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                : h.title.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);

        if (t.notes != null && !t.notes.trim().isEmpty()) {
            h.notes.setText(t.notes);
            h.notes.setVisibility(View.VISIBLE);
        } else {
            h.notes.setVisibility(View.GONE);
        }

        if (t.dueMillis > 0) {
            h.time.setText(DateFormat.format("EEE, d MMM, h:mm a", t.dueMillis));
        } else {
            h.time.setText(R.string.no_time);
        }

        h.check.setOnCheckedChangeListener(null);
        h.check.setChecked(t.done);
        h.check.setOnCheckedChangeListener((b, checked) -> listener.onToggle(t, checked));
        h.edit.setOnClickListener(v -> listener.onEdit(t));
        h.delete.setOnClickListener(v -> listener.onDelete(t));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final CheckBox check;
        final TextView title;
        final TextView notes;
        final TextView time;
        final Button edit;
        final Button delete;

        Holder(View v) {
            super(v);
            check = v.findViewById(R.id.cbDone);
            title = v.findViewById(R.id.tvTitle);
            notes = v.findViewById(R.id.tvNotes);
            time = v.findViewById(R.id.tvTime);
            edit = v.findViewById(R.id.btnEdit);
            delete = v.findViewById(R.id.btnDelete);
        }
    }
}

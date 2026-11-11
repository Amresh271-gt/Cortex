package com.amresh.cortex;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Read-only list of deleted tasks. They stay for 15 days and are then removed automatically. */
public class BinAdapter extends RecyclerView.Adapter<BinAdapter.Holder> {

    private static final long DAY = 24L * 60 * 60 * 1000;

    private final List<Task> items = new ArrayList<>();

    public void setTasks(List<Task> tasks) {
        items.clear();
        if (tasks != null) items.addAll(tasks);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bin, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Task t = items.get(position);
        h.title.setText(t.title);

        if (t.notes != null && !t.notes.trim().isEmpty()) {
            h.notes.setText(t.notes);
            h.notes.setVisibility(View.VISIBLE);
        } else {
            h.notes.setVisibility(View.GONE);
        }

        long remaining = t.deletedAt + AppDatabase.BIN_RETENTION_MILLIS - System.currentTimeMillis();
        long daysLeft = Math.max(0, (remaining + DAY - 1) / DAY);

        StringBuilder sb = new StringBuilder();
        sb.append(t.done ? "Completed" : "Not completed");
        sb.append(" • Deleted ").append(DateFormat.format("d MMM, h:mm a", t.deletedAt));
        sb.append(" • ").append(daysLeft).append(daysLeft == 1 ? " day left" : " days left");
        h.info.setText(sb.toString());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView notes;
        final TextView info;

        Holder(View v) {
            super(v);
            title = v.findViewById(R.id.tvBinTitle);
            notes = v.findViewById(R.id.tvBinNotes);
            info = v.findViewById(R.id.tvBinInfo);
        }
    }
}

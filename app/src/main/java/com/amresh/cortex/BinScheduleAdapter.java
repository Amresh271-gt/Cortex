package com.amresh.cortex;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Past schedule entries in the bin. Kept for 15 days after their day passed, then removed. */
public class BinScheduleAdapter extends RecyclerView.Adapter<BinScheduleAdapter.Holder> {

    public interface Listener {
        void onDeleteForever(ScheduleEntry entry);
    }

    private static final long DAY = 24L * 60 * 60 * 1000;

    private final List<ScheduleEntry> items = new ArrayList<>();
    private final Listener listener;

    public BinScheduleAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setEntries(List<ScheduleEntry> entries) {
        items.clear();
        if (entries != null) items.addAll(entries);
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
        final ScheduleEntry e = items.get(position);
        h.title.setText(e.title);
        h.notes.setVisibility(View.GONE);
        h.delete.setOnClickListener(v -> listener.onDeleteForever(e));

        long remaining = e.archivedAt + ScheduleManager.BIN_RETENTION_MILLIS - System.currentTimeMillis();
        long daysLeft = Math.max(0, (remaining + DAY - 1) / DAY);

        StringBuilder sb = new StringBuilder();
        sb.append(DateFormat.format("EEE, d MMM", e.dayStart));
        if (e.timeMillis > 0) {
            sb.append(", ").append(DateFormat.format("h:mm a", e.timeMillis));
        }
        sb.append(e.taskId > 0 ? " • From task" : " • Added here");
        if (e.done) sb.append(" • Completed");
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
        final Button delete;

        Holder(View v) {
            super(v);
            title = v.findViewById(R.id.tvBinTitle);
            notes = v.findViewById(R.id.tvBinNotes);
            info = v.findViewById(R.id.tvBinInfo);
            delete = v.findViewById(R.id.btnBinDelete);
        }
    }
}

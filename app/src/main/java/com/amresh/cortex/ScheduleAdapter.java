package com.amresh.cortex;

import android.graphics.Paint;
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

/** One day of the schedule as a list: time, title, where it came from, and a remove button. */
public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.Holder> {

    public interface Listener {
        void onRemove(ScheduleEntry entry);
    }

    private final List<ScheduleEntry> items = new ArrayList<>();
    private final Listener listener;

    public ScheduleAdapter(Listener listener) {
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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final ScheduleEntry e = items.get(position);
        h.title.setText(e.title);
        h.title.setPaintFlags(e.done
                ? h.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : h.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

        if (e.timeMillis > 0) {
            h.time.setText(DateFormat.format("h:mm a", e.timeMillis));
        } else {
            h.time.setText(R.string.schedule_any_time);
        }

        h.source.setText(e.taskId > 0 ? R.string.schedule_from_task : R.string.schedule_added_here);
        h.remove.setOnClickListener(v -> listener.onRemove(e));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView time;
        final TextView title;
        final TextView source;
        final Button remove;

        Holder(View v) {
            super(v);
            time = v.findViewById(R.id.tvSchedItemTime);
            title = v.findViewById(R.id.tvSchedItemTitle);
            source = v.findViewById(R.id.tvSchedItemSource);
            remove = v.findViewById(R.id.btnSchedRemove);
        }
    }
}

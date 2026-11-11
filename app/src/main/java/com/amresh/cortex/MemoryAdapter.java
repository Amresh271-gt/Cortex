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

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.Holder> {

    public interface Listener {
        /** Tapping the row opens the read-only view. */
        void onView(Memory memory);

        void onPin(Memory memory);

        void onCopy(Memory memory);

        void onEdit(Memory memory);

        void onDelete(Memory memory);
    }

    private final List<Memory> items = new ArrayList<>();
    private final Listener listener;

    public MemoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Memory> memories) {
        items.clear();
        if (memories != null) items.addAll(memories);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_memory, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final Memory m = items.get(position);
        h.title.setText(m.title);

        if (m.content != null && !m.content.trim().isEmpty()) {
            h.content.setText(m.content);
            h.content.setVisibility(View.VISIBLE);
        } else {
            h.content.setVisibility(View.GONE);
        }

        boolean hasTag = m.tag != null && !m.tag.trim().isEmpty();
        String tagText = hasTag ? m.tag : h.itemView.getContext().getString(R.string.mem_untagged);
        h.meta.setText(h.itemView.getContext().getString(R.string.mem_meta_format,
                tagText, DateFormat.format("d MMM yyyy", m.createdAt)));

        // Pin looks "on" when pinned and faded when not.
        h.pin.setAlpha(m.pinned ? 1f : 0.3f);

        h.itemView.setOnClickListener(v -> listener.onView(m));
        h.pin.setOnClickListener(v -> listener.onPin(m));
        h.copy.setOnClickListener(v -> listener.onCopy(m));
        h.edit.setOnClickListener(v -> listener.onEdit(m));
        h.delete.setOnClickListener(v -> listener.onDelete(m));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView content;
        final TextView meta;
        final Button pin;
        final Button copy;
        final Button edit;
        final Button delete;

        Holder(View v) {
            super(v);
            title = v.findViewById(R.id.tvMemTitle);
            content = v.findViewById(R.id.tvMemContent);
            meta = v.findViewById(R.id.tvMemMeta);
            pin = v.findViewById(R.id.btnMemPin);
            copy = v.findViewById(R.id.btnMemCopy);
            edit = v.findViewById(R.id.btnMemEdit);
            delete = v.findViewById(R.id.btnMemDelete);
        }
    }
}

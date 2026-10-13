package com.example.biteforlessfoodbridge.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.model.ActivityEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Session activity-log rows. */
public class ActivityEventAdapter extends RecyclerView.Adapter<ActivityEventAdapter.Row> {

    private final List<ActivityEvent> rows = new ArrayList<>();

    public void setRows(List<ActivityEvent> newRows) {
        rows.clear();
        if (newRows != null) rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_event, parent, false);
        return new Row(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Row h, int position) {
        ActivityEvent e = rows.get(position);
        h.type.setText(e.getId() + " · " + e.getType());
        h.text.setText(e.getText());
        SimpleDateFormat fmt = new SimpleDateFormat("EEE d MMM, HH:mm:ss", Locale.getDefault());
        h.meta.setText(e.getActor() + " · " + fmt.format(new Date(e.getAtEpoch())));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class Row extends RecyclerView.ViewHolder {
        final TextView type;
        final TextView text;
        final TextView meta;

        Row(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.alog_type);
            text = itemView.findViewById(R.id.alog_text);
            meta = itemView.findViewById(R.id.alog_meta);
        }
    }
}

package com.example.biteforlessfoodbridge.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.util.AdminStats;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Observed-activity customer directory rows. */
public class AdminCustomerAdapter extends RecyclerView.Adapter<AdminCustomerAdapter.Row> {

    private final List<AdminStats.CustomerSummary> rows = new ArrayList<>();

    public void setRows(List<AdminStats.CustomerSummary> newRows) {
        rows.clear();
        if (newRows != null) rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_customer, parent, false);
        return new Row(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Row h, int position) {
        AdminStats.CustomerSummary c = rows.get(position);
        h.id.setText(c.userId);
        h.stats.setText(c.reservations + " reservations · " + c.completed + " completed");
        SimpleDateFormat fmt = new SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault());
        h.last.setText("Last activity: " + (c.lastEpoch > 0
                ? fmt.format(new Date(c.lastEpoch)) : "—"));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class Row extends RecyclerView.ViewHolder {
        final TextView id;
        final TextView stats;
        final TextView last;

        Row(@NonNull View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.acustomer_id);
            stats = itemView.findViewById(R.id.acustomer_stats);
            last = itemView.findViewById(R.id.acustomer_last);
        }
    }
}

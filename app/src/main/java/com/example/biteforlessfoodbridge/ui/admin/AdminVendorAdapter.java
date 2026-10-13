package com.example.biteforlessfoodbridge.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.model.VendorStatus;

import java.util.ArrayList;
import java.util.List;

/** Vendor directory rows with contextual approve/suspend actions. */
public class AdminVendorAdapter extends RecyclerView.Adapter<AdminVendorAdapter.Row> {

    public interface Listener {
        void onDetails(Vendor vendor);
        void onAction(Vendor vendor);
    }

    public static class VendorRow {
        public final Vendor vendor;
        public final int listingCount;

        public VendorRow(Vendor vendor, int listingCount) {
            this.vendor = vendor;
            this.listingCount = listingCount;
        }
    }

    private final List<VendorRow> rows = new ArrayList<>();
    private final Listener listener;

    public AdminVendorAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setRows(List<VendorRow> newRows) {
        rows.clear();
        if (newRows != null) rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_vendor, parent, false);
        return new Row(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Row h, int position) {
        VendorRow row = rows.get(position);
        Vendor v = row.vendor;
        h.name.setText(v.getName());
        h.info.setText(v.getLocation() + " · " + row.listingCount + " listings");
        h.status.setText(v.getStatus().name());
        int color;
        if (v.getStatus() == VendorStatus.APPROVED) {
            h.action.setText("Suspend");
            color = h.itemView.getContext().getResources().getColor(R.color.success, null);
        } else if (v.getStatus() == VendorStatus.PENDING) {
            h.action.setText("Approve");
            color = h.itemView.getContext().getResources().getColor(R.color.food_yellow, null);
        } else {
            h.action.setText("Reactivate");
            color = h.itemView.getContext().getResources().getColor(R.color.food_red, null);
        }
        h.status.setTextColor(color);
        h.details.setOnClickListener(view -> {
            if (listener != null) listener.onDetails(v);
        });
        h.action.setOnClickListener(view -> {
            if (listener != null) listener.onAction(v);
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class Row extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView status;
        final TextView info;
        final Button details;
        final Button action;

        Row(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.avendor_name);
            status = itemView.findViewById(R.id.avendor_status);
            info = itemView.findViewById(R.id.avendor_info);
            details = itemView.findViewById(R.id.avendor_details);
            action = itemView.findViewById(R.id.avendor_action);
        }
    }
}

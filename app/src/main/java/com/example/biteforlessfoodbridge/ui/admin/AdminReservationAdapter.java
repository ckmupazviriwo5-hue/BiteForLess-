package com.example.biteforlessfoodbridge.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.ReservationStatus;
import com.example.biteforlessfoodbridge.util.DateTimeUtil;
import com.example.biteforlessfoodbridge.util.PriceFormat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** View-only all-platform reservation overview (no lifecycle bypass). */
public class AdminReservationAdapter extends RecyclerView.Adapter<AdminReservationAdapter.Row> {

    public static class AdminResRow {
        public final Reservation reservation;
        public final String listingName;
        public final String vendorName;
        public final double unitPrice;
        public final long deadlineEpoch;

        public AdminResRow(Reservation reservation, String listingName, String vendorName,
                           double unitPrice, long deadlineEpoch) {
            this.reservation = reservation;
            this.listingName = listingName;
            this.vendorName = vendorName;
            this.unitPrice = unitPrice;
            this.deadlineEpoch = deadlineEpoch;
        }
    }

    private final List<AdminResRow> rows = new ArrayList<>();

    public void setRows(List<AdminResRow> newRows) {
        rows.clear();
        if (newRows != null) rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vendor_reservation, parent, false);
        return new Row(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Row h, int position) {
        AdminResRow row = rows.get(position);
        Reservation r = row.reservation;

        h.ref.setText(r.getId() + " · " + r.getPickupCode());
        h.title.setText(row.listingName + " × " + r.getQuantity()
                + " · " + PriceFormat.format(row.unitPrice * r.getQuantity()));
        h.customer.setText((r.getCustomerName().isEmpty() ? "Customer" : r.getCustomerName())
                + " · " + row.vendorName);
        SimpleDateFormat fmt = new SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault());
        h.meta.setText("Booked " + fmt.format(new Date(r.getCreatedAtEpoch()))
                + " · " + DateTimeUtil.formatDeadline(row.deadlineEpoch));

        int color;
        if (r.getStatus() == ReservationStatus.RESERVED) {
            h.status.setText("RESERVED");
            color = h.itemView.getContext().getResources().getColor(R.color.success, null);
        } else if (r.getStatus() == ReservationStatus.COMPLETED) {
            h.status.setText("COMPLETED");
            color = h.itemView.getContext().getResources().getColor(R.color.info, null);
        } else if (r.getStatus() == ReservationStatus.CANCELLED) {
            h.status.setText("CANCELLED");
            color = h.itemView.getContext().getResources().getColor(R.color.text_hint, null);
        } else {
            h.status.setText("EXPIRED");
            color = h.itemView.getContext().getResources().getColor(R.color.food_red, null);
        }
        h.status.setTextColor(color);
        // No admin action button: lifecycle rules apply, no admin cancellation this phase.
        h.complete.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class Row extends RecyclerView.ViewHolder {
        final TextView ref;
        final TextView title;
        final TextView customer;
        final TextView meta;
        final TextView status;
        final View complete;

        Row(@NonNull View itemView) {
            super(itemView);
            ref = itemView.findViewById(R.id.vres_ref);
            title = itemView.findViewById(R.id.vres_title);
            customer = itemView.findViewById(R.id.vres_customer);
            meta = itemView.findViewById(R.id.vres_meta);
            status = itemView.findViewById(R.id.vres_status);
            complete = itemView.findViewById(R.id.vres_complete);
        }
    }
}

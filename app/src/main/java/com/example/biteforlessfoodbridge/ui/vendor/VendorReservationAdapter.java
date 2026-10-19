package com.example.biteforlessfoodbridge.ui.vendor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.ReservationStatus;
import com.example.biteforlessfoodbridge.util.DateTimeUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Reservations on the vendor's own listings with simulated pickup action. */
public class VendorReservationAdapter extends RecyclerView.Adapter<VendorReservationAdapter.Row> {

    public interface Listener {
        void onComplete(Reservation reservation);
    }

    public static class VendorResRow {
        public final Reservation reservation;
        public final String listingName;
        public final long deadlineEpoch;
        /** Customer display name when the backend provides one ("" in demo). */
        public final String customerName;

        public VendorResRow(Reservation reservation, String listingName, long deadlineEpoch) {
            this(reservation, listingName, deadlineEpoch, "");
        }

        public VendorResRow(Reservation reservation, String listingName, long deadlineEpoch,
                            String customerName) {
            this.reservation = reservation;
            this.listingName = listingName;
            this.deadlineEpoch = deadlineEpoch;
            this.customerName = customerName == null ? "" : customerName;
        }
    }

    private final List<VendorResRow> rows = new ArrayList<>();
    private final Listener listener;

    public VendorReservationAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setRows(List<VendorResRow> newRows) {
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
        VendorResRow row = rows.get(position);
        Reservation r = row.reservation;

        h.ref.setText(r.getPickupCode().isEmpty() ? r.getId() : r.getPickupCode());
        h.title.setText(row.listingName + " × " + r.getQuantity());
        // NEVER render the pickup PIN here — the vendor must collect it from
        // the customer at verification time (contract §6).
        String who = row.customerName.isEmpty() ? "Customer" : row.customerName;
        h.customer.setText("Customer: " + who);
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

        boolean canComplete = r.getStatus() == ReservationStatus.RESERVED;
        h.complete.setText("Verify pickup");
        h.complete.setVisibility(canComplete ? View.VISIBLE : View.GONE);
        h.complete.setOnClickListener(canComplete ? v -> {
            if (listener != null) listener.onComplete(r);
        } : null);
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
        final Button complete;

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

package com.example.biteforlessfoodbridge.ui.customer;

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
import com.example.biteforlessfoodbridge.util.PriceFormat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Binds reservations to {@code item_reservation_card}. No business rules here. */
public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ResViewHolder> {

    public interface OnCancelClick {
        void onCancelClick(Reservation reservation);
    }

    /** Display row assembled by the owning Fragment from the shared repository. */
    public static class ReservationRow {
        public final Reservation reservation;
        public final String listingName;
        public final String vendorName;
        public final double unitPrice;
        public final long deadlineEpoch;
        public final String location;

        public ReservationRow(Reservation reservation, String listingName, String vendorName,
                              double unitPrice, long deadlineEpoch, String location) {
            this.reservation = reservation;
            this.listingName = listingName;
            this.vendorName = vendorName;
            this.unitPrice = unitPrice;
            this.deadlineEpoch = deadlineEpoch;
            this.location = location;
        }
    }

    private final List<ReservationRow> rows = new ArrayList<>();
    private final OnCancelClick listener;

    public ReservationAdapter(OnCancelClick listener) {
        this.listener = listener;
    }

    public void setRows(List<ReservationRow> newRows) {
        rows.clear();
        if (newRows != null) rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ResViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation_card, parent, false);
        return new ResViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ResViewHolder h, int position) {
        ReservationRow row = rows.get(position);
        Reservation r = row.reservation;

        h.ref.setText(r.getPickupCode().isEmpty() ? r.getId() : r.getPickupCode());
        h.title.setText(row.listingName + " × " + r.getQuantity());
        h.vendor.setText(row.vendorName + " · " + row.location);
        h.total.setText(PriceFormat.format(row.unitPrice * r.getQuantity()));
        SimpleDateFormat fmt = new SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault());
        h.meta.setText("Booked " + fmt.format(new Date(r.getCreatedAtEpoch()))
                + " · " + DateTimeUtil.formatDeadline(row.deadlineEpoch));

        int color;
        String label;
        if (r.getStatus() == ReservationStatus.RESERVED) {
            label = "RESERVED";
            color = h.itemView.getContext().getResources().getColor(R.color.success, null);
        } else if (r.getStatus() == ReservationStatus.COMPLETED) {
            label = "COMPLETED";
            color = h.itemView.getContext().getResources().getColor(R.color.info, null);
        } else if (r.getStatus() == ReservationStatus.CANCELLED) {
            label = "CANCELLED";
            color = h.itemView.getContext().getResources().getColor(R.color.text_hint, null);
        } else {
            label = "EXPIRED";
            color = h.itemView.getContext().getResources().getColor(R.color.food_red, null);
        }
        h.status.setText(label);
        h.status.setTextColor(color);

        boolean canCancel = r.getStatus() == ReservationStatus.RESERVED;
        h.cancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);
        if (canCancel) {
            h.cancel.setOnClickListener(v -> {
                if (listener != null) listener.onCancelClick(r);
            });
        } else {
            h.cancel.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ResViewHolder extends RecyclerView.ViewHolder {
        final TextView ref;
        final TextView title;
        final TextView vendor;
        final TextView total;
        final TextView meta;
        final TextView status;
        final Button cancel;

        ResViewHolder(@NonNull View itemView) {
            super(itemView);
            ref = itemView.findViewById(R.id.res_ref);
            title = itemView.findViewById(R.id.res_title);
            vendor = itemView.findViewById(R.id.res_vendor);
            total = itemView.findViewById(R.id.res_total);
            meta = itemView.findViewById(R.id.res_meta);
            status = itemView.findViewById(R.id.res_status);
            cancel = itemView.findViewById(R.id.res_cancel);
        }
    }
}

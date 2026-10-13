package com.example.biteforlessfoodbridge.ui.vendor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.util.DateTimeUtil;
import com.example.biteforlessfoodbridge.util.PriceFormat;

import java.util.ArrayList;
import java.util.List;

/** Vendor's own listings with edit / activate-deactivate actions. */
public class VendorListingAdapter extends RecyclerView.Adapter<VendorListingAdapter.Row> {

    public interface Listener {
        void onEdit(FoodListing listing);
        void onToggleStatus(FoodListing listing);
    }

    private final List<FoodListing> rows = new ArrayList<>();
    private final Listener listener;

    public VendorListingAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setRows(List<FoodListing> newRows) {
        rows.clear();
        if (newRows != null) rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vendor_listing, parent, false);
        return new Row(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Row h, int position) {
        FoodListing l = rows.get(position);
        long now = System.currentTimeMillis();

        h.name.setText(l.getName());
        h.info.setText(PriceFormat.format(l.getDiscountedPrice())
                + " (was " + PriceFormat.format(l.getOriginalPrice()) + ") · "
                + PriceFormat.discountBadge(l.getOriginalPrice(), l.getDiscountedPrice()));

        String statusText;
        if (l.isExpired(now)) statusText = "Expired";
        else if (l.getStatus() != ListingStatus.ACTIVE) statusText = "Paused";
        else if (l.getAvailableQuantity() <= 0) statusText = "Sold out";
        else statusText = "Active";
        h.stock.setText(l.getAvailableQuantity() + " in stock · " + statusText
                + " · " + DateTimeUtil.formatDeadline(l.getCollectionDeadlineEpoch()));

        h.toggle.setText(l.getStatus() == ListingStatus.ACTIVE ? "Deactivate" : "Activate");
        h.edit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(l);
        });
        h.toggle.setOnClickListener(v -> {
            if (listener != null) listener.onToggleStatus(l);
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class Row extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView info;
        final TextView stock;
        final Button edit;
        final Button toggle;

        Row(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.vlisting_name);
            info = itemView.findViewById(R.id.vlisting_info);
            stock = itemView.findViewById(R.id.vlisting_stock);
            edit = itemView.findViewById(R.id.vlisting_edit);
            toggle = itemView.findViewById(R.id.vlisting_toggle);
        }
    }
}

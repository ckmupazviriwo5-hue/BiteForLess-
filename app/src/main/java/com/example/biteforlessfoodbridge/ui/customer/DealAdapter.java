package com.example.biteforlessfoodbridge.ui.customer;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

/** Binds food listings to {@code item_food_card}. No business rules here. */
public class DealAdapter extends RecyclerView.Adapter<DealAdapter.DealViewHolder> {

    public interface OnDealClick {
        void onDealClick(String listingId);
    }

    /** Display row assembled by the owning Fragment from the shared repository. */
    public static class DealRow {
        public final FoodListing listing;
        public final String vendorName;
        public final String categoryName;

        public DealRow(FoodListing listing, String vendorName, String categoryName) {
            this.listing = listing;
            this.vendorName = vendorName;
            this.categoryName = categoryName;
        }
    }

    private final List<DealRow> rows = new ArrayList<>();
    private final OnDealClick listener;

    public DealAdapter(OnDealClick listener) {
        this.listener = listener;
    }

    public void setRows(List<DealRow> newRows) {
        rows.clear();
        if (newRows != null) rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_card, parent, false);
        return new DealViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DealViewHolder h, int position) {
        DealRow row = rows.get(position);
        FoodListing l = row.listing;
        long now = System.currentTimeMillis();
        boolean reservable = l.isReservable(now);

        h.image.setImageResource(R.drawable.ic_logo_bite);
        h.name.setText(l.getName());
        h.vendor.setText(row.vendorName + " · " + l.getLocation());
        h.price.setText(PriceFormat.format(l.getDiscountedPrice()));
        h.originalPrice.setText(PriceFormat.format(l.getOriginalPrice()));
        h.originalPrice.setPaintFlags(
                h.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        if (l.isExpired(now)) {
            h.badge.setText("Expired");
            h.badge.setBackgroundColor(h.itemView.getContext()
                    .getResources().getColor(R.color.food_red, null));
        } else if (l.getStatus() != ListingStatus.ACTIVE) {
            h.badge.setText("Paused");
            h.badge.setBackgroundColor(h.itemView.getContext()
                    .getResources().getColor(R.color.text_hint, null));
        } else if (l.getAvailableQuantity() <= 0) {
            h.badge.setText("Sold out");
            h.badge.setBackgroundColor(h.itemView.getContext()
                    .getResources().getColor(R.color.food_red, null));
        } else {
            String badge = PriceFormat.discountBadge(l.getOriginalPrice(), l.getDiscountedPrice());
            h.badge.setText(badge.isEmpty() ? row.categoryName : badge + " · " + row.categoryName);
            h.badge.setBackgroundColor(h.itemView.getContext()
                    .getResources().getColor(R.color.food_yellow, null));
        }

        String stock = l.getAvailableQuantity() <= 0
                ? "Out of stock"
                : l.getAvailableQuantity() + " left";
        h.meta.setText(stock + " · " + DateTimeUtil.formatDeadline(l.getCollectionDeadlineEpoch()));

        h.itemView.setAlpha(reservable ? 1.0f : 0.6f);
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDealClick(l.getId());
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class DealViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView badge;
        final TextView name;
        final TextView vendor;
        final TextView price;
        final TextView originalPrice;
        final TextView meta;

        DealViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.deal_image);
            badge = itemView.findViewById(R.id.deal_badge);
            name = itemView.findViewById(R.id.deal_name);
            vendor = itemView.findViewById(R.id.deal_vendor);
            price = itemView.findViewById(R.id.deal_price);
            originalPrice = itemView.findViewById(R.id.deal_original_price);
            meta = itemView.findViewById(R.id.deal_meta);
        }
    }
}

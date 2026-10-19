package com.example.biteforlessfoodbridge.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.model.Review;
import com.example.biteforlessfoodbridge.model.ReviewStatus;

import java.util.ArrayList;
import java.util.List;

/** Review moderation rows (hide/show preserves the record). */
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.Row> {

    public interface Listener {
        void onToggle(Review review);
    }

    public static class ReviewRow {
        public final Review review;
        public final String listingName;

        public ReviewRow(Review review, String listingName) {
            this.review = review;
            this.listingName = listingName;
        }
    }

    private final List<ReviewRow> rows = new ArrayList<>();
    private final Listener listener;

    public ReviewAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setRows(List<ReviewRow> newRows) {
        rows.clear();
        if (newRows != null) rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_review, parent, false);
        return new Row(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Row h, int position) {
        ReviewRow row = rows.get(position);
        Review r = row.review;
        h.listing.setText(row.listingName);
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) stars.append(i < r.getRating() ? "★" : "☆");
        h.stars.setText(stars.toString());
        h.text.setText(r.getText());
        String who = r.getUserName().isEmpty()
                ? (r.getUserId().isEmpty() ? "Customer" : r.getUserId()) : r.getUserName();
        h.user.setText(who);
        h.status.setText(r.getStatus().name());
        int color;
        if (r.getStatus() == ReviewStatus.VISIBLE) {
            h.toggle.setText("Hide");
            color = h.itemView.getContext().getResources().getColor(R.color.success, null);
        } else if (r.getStatus() == ReviewStatus.HIDDEN) {
            h.toggle.setText("Show");
            color = h.itemView.getContext().getResources().getColor(R.color.food_red, null);
        } else {
            h.toggle.setText("Approve");
            color = h.itemView.getContext().getResources().getColor(R.color.food_yellow, null);
        }
        h.status.setTextColor(color);
        h.toggle.setOnClickListener(v -> {
            if (listener != null) listener.onToggle(r);
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class Row extends RecyclerView.ViewHolder {
        final TextView listing;
        final TextView status;
        final TextView stars;
        final TextView text;
        final TextView user;
        final Button toggle;

        Row(@NonNull View itemView) {
            super(itemView);
            listing = itemView.findViewById(R.id.areview_listing);
            status = itemView.findViewById(R.id.areview_status);
            stars = itemView.findViewById(R.id.areview_stars);
            text = itemView.findViewById(R.id.areview_text);
            user = itemView.findViewById(R.id.areview_user);
            toggle = itemView.findViewById(R.id.areview_toggle);
        }
    }
}

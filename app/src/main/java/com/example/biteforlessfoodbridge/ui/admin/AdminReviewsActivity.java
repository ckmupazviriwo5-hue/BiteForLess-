package com.example.biteforlessfoodbridge.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.data.RepositoryProvider;
import com.example.biteforlessfoodbridge.data.SessionManager;
import com.example.biteforlessfoodbridge.data.remote.RemoteRepository;
import com.example.biteforlessfoodbridge.data.remote.SupaResult;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.Review;
import com.example.biteforlessfoodbridge.model.ReviewStatus;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/** Sample-review moderation: hide/show/approve, records preserved. */
public class AdminReviewsActivity extends AppCompatActivity implements ReviewAdapter.Listener {

    private DemoRepository repo;
    private String actor;
    private boolean live;
    private final List<Review> cachedReviews = new ArrayList<>();
    private final List<FoodListing> cachedListings = new ArrayList<>();
    private ReviewAdapter adapter;
    private RecyclerView list;
    private View empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reviews);

        repo = DemoRepository.getInstance();
        live = RepositoryProvider.isLive(this);
        SessionManager session = new SessionManager(this);
        String email = session.getEmail();
        actor = email == null || email.isEmpty() ? "admin (demo)" : email;

        MaterialToolbar toolbar = findViewById(R.id.admin_reviews_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        list = findViewById(R.id.admin_reviews_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewAdapter(this);
        list.setAdapter(adapter);
        empty = findViewById(R.id.admin_reviews_empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSource();
    }

    private void loadSource() {
        if (!live) {
            cachedReviews.clear();
            cachedReviews.addAll(repo.getReviews());
            cachedListings.clear();
            cachedListings.addAll(repo.getAll());
            refresh();
            return;
        }
        View overlay = LoadingOverlay.show(this);
        RemoteRepository remote = RepositoryProvider.remote(this);
        remote.getAllReviews(new SupaResult<List<Review>>() {
            @Override public void onSuccess(List<Review> reviews) {
                remote.getAllListings(new SupaResult<List<FoodListing>>() {
                    @Override public void onSuccess(List<FoodListing> listings) {
                        LoadingOverlay.hide(overlay);
                        cachedReviews.clear();
                        cachedReviews.addAll(reviews);
                        cachedListings.clear();
                        cachedListings.addAll(listings);
                        refresh();
                    }
                    @Override public void onError(String m) {
                        LoadingOverlay.hide(overlay);
                        LoadingOverlay.error(AdminReviewsActivity.this, m,
                                () -> loadSource());
                    }
                });
            }
            @Override public void onError(String m) {
                LoadingOverlay.hide(overlay);
                LoadingOverlay.error(AdminReviewsActivity.this, m, () -> loadSource());
            }
        });
    }

    private void refresh() {
        if (adapter == null) return;
        java.util.Map<String, String> names = new java.util.HashMap<>();
        for (FoodListing l : cachedListings) names.put(l.getId(), l.getName());
        List<ReviewAdapter.ReviewRow> rows = new ArrayList<>();
        for (Review r : cachedReviews) {
            rows.add(new ReviewAdapter.ReviewRow(r,
                    names.getOrDefault(r.getListingId(), "Listing unavailable")));
        }
        adapter.setRows(rows);
        boolean hasRows = !rows.isEmpty();
        list.setVisibility(hasRows ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasRows ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onToggle(Review review) {
        ReviewStatus target;
        String verb;
        if (review.getStatus() == ReviewStatus.VISIBLE) {
            target = ReviewStatus.HIDDEN;
            verb = "hide";
        } else if (review.getStatus() == ReviewStatus.HIDDEN) {
            target = ReviewStatus.VISIBLE;
            verb = "show";
        } else {
            target = ReviewStatus.VISIBLE;
            verb = "approve";
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm")
                .setMessage("Do you want to " + verb + " this sample review?"
                        + " The record is preserved either way.")
                .setPositiveButton("Confirm", (d, w) -> {
                    if (live) {
                        View overlay = LoadingOverlay.show(this);
                        RepositoryProvider.remote(this).rpcModerateReview(
                                review.getId(), target, new SupaResult<String>() {
                                    @Override public void onSuccess(String v) {
                                        LoadingOverlay.hide(overlay);
                                        Toast.makeText(AdminReviewsActivity.this,
                                                "Review " + verb + "d",
                                                Toast.LENGTH_SHORT).show();
                                        loadSource();
                                    }
                                    @Override public void onError(String m) {
                                        LoadingOverlay.hide(overlay);
                                        Toast.makeText(AdminReviewsActivity.this, m,
                                                Toast.LENGTH_LONG).show();
                                        loadSource();
                                    }
                                });
                        return;
                    }
                    try {
                        repo.setReviewStatus(review.getId(), target, actor);
                        Toast.makeText(this, "Review " + verb + "d (demo)",
                                Toast.LENGTH_SHORT).show();
                    } catch (IllegalStateException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

package com.example.biteforlessfoodbridge.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.data.RepositoryProvider;
import com.example.biteforlessfoodbridge.data.remote.RemoteRepository;
import com.example.biteforlessfoodbridge.data.remote.SupaResult;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.Review;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.example.biteforlessfoodbridge.util.AdminStats;
import com.example.biteforlessfoodbridge.util.PriceFormat;

import java.util.List;

/** Platform overview with real local metrics + management entry points. */
public class AdminDashboardFragment extends Fragment {

    private DemoRepository repo;
    private GridLayout metrics;
    private TextView summaryLine;
    private boolean live;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
        repo = DemoRepository.getInstance();
        live = RepositoryProvider.isLive(requireContext());
        metrics = root.findViewById(R.id.admin_metrics);
        summaryLine = root.findViewById(R.id.admin_summary_line);

        root.findViewById(R.id.admin_open_vendors).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminVendorsActivity.class)));
        root.findViewById(R.id.admin_open_listings).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminListingsActivity.class)));
        root.findViewById(R.id.admin_open_reservations).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminReservationsActivity.class)));
        root.findViewById(R.id.admin_open_customers).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminCustomersActivity.class)));
        root.findViewById(R.id.admin_open_reviews).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminReviewsActivity.class)));
        root.findViewById(R.id.admin_open_log).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminActivityLogActivity.class)));

        refresh();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (repo != null) refresh();
    }

    private void refresh() {
        if (getContext() == null) return;
        if (live) {
            refreshLive();
            return;
        }
        showSummary(AdminStats.compute(repo.getVendors(), repo.getAll(),
                repo.getAllReservations(), repo.getReviews()), true);
    }

    private void refreshLive() {
        if (getActivity() == null) return;
        View overlay = LoadingOverlay.show(getActivity());
        RemoteRepository remote = RepositoryProvider.remote(requireContext());
        remote.getVendors(null, new SupaResult<List<Vendor>>() {
            @Override public void onSuccess(List<Vendor> vendors) {
                remote.getAllListings(new SupaResult<List<FoodListing>>() {
                    @Override public void onSuccess(List<FoodListing> listings) {
                        remote.getAllReservations(new SupaResult<List<Reservation>>() {
                            @Override public void onSuccess(List<Reservation> reservations) {
                                remote.getAllReviews(new SupaResult<List<Review>>() {
                                    @Override public void onSuccess(List<Review> reviews) {
                                        LoadingOverlay.hide(overlay);
                                        showSummary(AdminStats.compute(vendors, listings,
                                                reservations, reviews), false);
                                    }
                                    @Override public void onError(String m) {
                                        LoadingOverlay.hide(overlay);
                                        LoadingOverlay.error(getActivity(), m,
                                                () -> refresh());
                                    }
                                });
                            }
                            @Override public void onError(String m) {
                                LoadingOverlay.hide(overlay);
                                LoadingOverlay.error(getActivity(), m, () -> refresh());
                            }
                        });
                    }
                    @Override public void onError(String m) {
                        LoadingOverlay.hide(overlay);
                        LoadingOverlay.error(getActivity(), m, () -> refresh());
                    }
                });
            }
            @Override public void onError(String m) {
                LoadingOverlay.hide(overlay);
                LoadingOverlay.error(getActivity(), m, () -> refresh());
            }
        });
    }

    private void showSummary(AdminStats.Summary s, boolean demo) {
        metrics.removeAllViews();
        addMetric(String.valueOf(s.totalVendors), "Vendors");
        String states = s.approvedVendors + " ok · " + s.suspendedVendors + " susp";
        if (s.rejectedVendors > 0) states += " · " + s.rejectedVendors + " rej";
        if (s.pendingVendors > 0) states += " · " + s.pendingVendors + " pending";
        addMetric(states, "Vendor states");
        addMetric(s.activeListings + "/" + s.totalListings, "Active listings");
        addMetric(String.valueOf(s.totalReservations), "Reservations");
        addMetric(String.valueOf(s.reservedQtyOutstanding), "Meals awaiting");
        addMetric(PriceFormat.format(s.estimatedSales), "Est. sales");

        summaryLine.setText("Platform (" + (demo ? "demo" : "live") + "): "
                + s.completedPickups + " pickups completed · "
                + s.distinctCustomers + " observed "
                + (demo ? "demo customers" : "customers") + " · "
                + s.reviewsVisible + " visible / " + s.reviewsPending + " pending / "
                + s.reviewsHidden + " hidden reviews");
    }

    private void addMetric(String value, String label) {
        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_metric_card, metrics, false);
        ((TextView) card.findViewById(R.id.metric_value)).setText(value);
        ((TextView) card.findViewById(R.id.metric_label)).setText(label);
        GridLayout.LayoutParams params =
                (GridLayout.LayoutParams) card.getLayoutParams();
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.width = 0;
        card.setLayoutParams(params);
        metrics.addView(card);
    }
}

package com.example.biteforlessfoodbridge.ui.vendor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.data.RepositoryProvider;
import com.example.biteforlessfoodbridge.data.SessionManager;
import com.example.biteforlessfoodbridge.data.remote.RemoteRepository;
import com.example.biteforlessfoodbridge.data.remote.SupaResult;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.example.biteforlessfoodbridge.util.PriceFormat;
import com.example.biteforlessfoodbridge.util.VendorStats;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Vendor dashboard for the mapped demo vendor: real metrics, listing CRUD
 * and an entry point to reservation handling. Same shared repository as the
 * customer marketplace.
 */
public class VendorDashboardFragment extends Fragment implements VendorListingAdapter.Listener {

    private DemoRepository repo;
    private SessionManager session;
    private String vendorId;
    private boolean live;

    private TextView vendorName;
    private TextView vendorLocation;
    private GridLayout metrics;
    private TextView rescueLine;
    private VendorListingAdapter adapter;
    private RecyclerView listings;
    private View empty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_vendor_dashboard, container, false);
        repo = DemoRepository.getInstance();
        session = new SessionManager(requireContext());
        live = RepositoryProvider.isLive(requireContext());
        vendorId = live ? "" : session.requireDemoVendorId();

        vendorName = root.findViewById(R.id.vendor_name);
        vendorLocation = root.findViewById(R.id.vendor_location);
        metrics = root.findViewById(R.id.vendor_metrics);
        rescueLine = root.findViewById(R.id.vendor_rescue_line);
        listings = root.findViewById(R.id.vendor_listings);
        empty = root.findViewById(R.id.vendor_empty);

        listings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VendorListingAdapter(this);
        listings.setAdapter(adapter);

        root.findViewById(R.id.vendor_add_listing).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ListingFormActivity.class)));
        root.findViewById(R.id.vendor_view_reservations).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), VendorReservationsActivity.class)));

        refresh();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (getContext() == null) return;
        if (live) {
            refreshLive();
            return;
        }
        vendorId = session.requireDemoVendorId();
        Vendor vendor = repo.getVendorById(vendorId);
        vendorName.setText(vendor == null ? "Demo vendor" : vendor.getName());
        vendorLocation.setText(vendor == null ? "" : vendor.getLocation());

        VendorStats.Summary s = VendorStats.compute(
                repo.getAll(), repo.getAllReservations(), vendorId);
        showMetrics(s, true);

        List<FoodListing> owned = repo.getListingsForVendor(vendorId);
        adapter.setRows(owned);
        listings.setVisibility(owned.isEmpty() ? View.GONE : View.VISIBLE);
        empty.setVisibility(owned.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Live dashboard: owned vendor + its data from the server. */
    private void refreshLive() {
        if (getActivity() == null) return;
        View overlay = LoadingOverlay.show(getActivity());
        RemoteRepository remote = RepositoryProvider.remote(requireContext());
        remote.getMyVendor(new SupaResult<Vendor>() {
            @Override public void onSuccess(Vendor vendor) {
                vendorId = vendor.getId();
                vendorName.setText(vendor.getName());
                vendorLocation.setText(vendor.getLocation());
                remote.getAllListings(new SupaResult<List<FoodListing>>() {
                    @Override public void onSuccess(List<FoodListing> all) {
                        remote.getVendorReservations(vendorId,
                                new SupaResult<List<Reservation>>() {
                                    @Override public void onSuccess(List<Reservation> mine) {
                                        LoadingOverlay.hide(overlay);
                                        List<FoodListing> owned = new ArrayList<>();
                                        for (FoodListing l : all) {
                                            if (vendorId.equals(l.getVendorId())) owned.add(l);
                                        }
                                        showMetrics(VendorStats.compute(
                                                all, mine, vendorId), false);
                                        adapter.setRows(owned);
                                        listings.setVisibility(
                                                owned.isEmpty() ? View.GONE : View.VISIBLE);
                                        empty.setVisibility(
                                                owned.isEmpty() ? View.VISIBLE : View.GONE);
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
                vendorName.setText("No vendor business found");
                vendorLocation.setText(m);
                adapter.setRows(new ArrayList<>());
                listings.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showMetrics(VendorStats.Summary s, boolean demo) {
        metrics.removeAllViews();
        addMetric(PriceFormat.format(s.estimatedSales), "Est. sales");
        addMetric(String.valueOf(s.completedPickups), "Pickups done");
        addMetric(String.valueOf(s.reservedCount), "Awaiting pickup");
        addMetric(String.valueOf(s.activeListings) + "/" + s.totalListings, "Active listings");
        addMetric(String.valueOf(s.availableStock), "Stock awaiting");
        addMetric(String.valueOf(s.totalReservations), "Reservations");

        rescueLine.setText("Food rescue" + (demo ? " (demo)" : " (live)") + ": "
                + s.rescuedItems + " meals collected · "
                + PriceFormat.format(s.customerSavings) + " saved by customers");
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

    @Override
    public void onEdit(FoodListing listing) {
        Intent intent = new Intent(requireContext(), ListingFormActivity.class);
        intent.putExtra(ListingFormActivity.EXTRA_LISTING_ID, listing.getId());
        startActivity(intent);
    }

    @Override
    public void onToggleStatus(FoodListing listing) {
        boolean activating = listing.getStatus() != ListingStatus.ACTIVE;
        String action = activating ? "activate" : "deactivate";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm")
                .setMessage("Do you want to " + action + " \"" + listing.getName() + "\"?"
                        + (activating ? "" : " Customers will no longer be able to reserve it;"
                        + " existing reservations stay visible."))
                .setPositiveButton("Confirm", (d, w) -> {
                    if (live) {
                        View overlay = LoadingOverlay.show(getActivity());
                        RepositoryProvider.remote(requireContext()).rpcSetListingStatus(
                                listing.getId(),
                                activating ? ListingStatus.ACTIVE : ListingStatus.INACTIVE,
                                new SupaResult<String>() {
                                    @Override public void onSuccess(String v) {
                                        LoadingOverlay.hide(overlay);
                                        Toast.makeText(requireContext(), "Listing updated",
                                                Toast.LENGTH_SHORT).show();
                                        refresh();
                                    }
                                    @Override public void onError(String m) {
                                        LoadingOverlay.hide(overlay);
                                        Toast.makeText(requireContext(), m,
                                                Toast.LENGTH_LONG).show();
                                        refresh();
                                    }
                                });
                        return;
                    }
                    try {
                        repo.setListingStatus(listing.getId(),
                                activating ? ListingStatus.ACTIVE : ListingStatus.INACTIVE,
                                vendorId);
                        Toast.makeText(requireContext(), "Listing updated (demo)",
                                Toast.LENGTH_SHORT).show();
                    } catch (IllegalStateException e) {
                        Toast.makeText(requireContext(), e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

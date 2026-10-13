package com.example.biteforlessfoodbridge.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.data.RepositoryProvider;
import com.example.biteforlessfoodbridge.data.remote.RemoteRepository;
import com.example.biteforlessfoodbridge.data.remote.SupaResult;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.ReservationStatus;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.example.biteforlessfoodbridge.util.AdminStats;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/** View-only all-platform reservation overview (no admin cancellation). */
public class AdminReservationsActivity extends AppCompatActivity {

    private static final String[] FILTERS = {"All", "Reserved", "Completed", "Cancelled", "Expired"};

    private DemoRepository repo;
    private boolean live;
    private final List<Reservation> cachedReservations = new ArrayList<>();
    private final List<FoodListing> cachedListings = new ArrayList<>();
    private final List<Vendor> cachedVendors = new ArrayList<>();
    private AdminReservationAdapter adapter;
    private TextInputEditText search;
    private RecyclerView list;
    private View empty;
    private ReservationStatus filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reservations);

        repo = DemoRepository.getInstance();
        live = RepositoryProvider.isLive(this);

        MaterialToolbar toolbar = findViewById(R.id.admin_res_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        search = findViewById(R.id.admin_res_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { refresh(); }
        });

        ChipGroup group = findViewById(R.id.admin_res_filter);
        for (int i = 0; i < FILTERS.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(FILTERS[i]);
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            group.addView(chip);
        }
        group.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                filter = null;
            } else {
                Chip chip = g.findViewById(checkedIds.get(0));
                String label = chip == null ? "All" : chip.getText().toString();
                if ("Reserved".equals(label)) filter = ReservationStatus.RESERVED;
                else if ("Completed".equals(label)) filter = ReservationStatus.COMPLETED;
                else if ("Cancelled".equals(label)) filter = ReservationStatus.CANCELLED;
                else if ("Expired".equals(label)) filter = ReservationStatus.EXPIRED;
                else filter = null;
            }
            refresh();
        });

        list = findViewById(R.id.admin_res_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminReservationAdapter();
        list.setAdapter(adapter);
        empty = findViewById(R.id.admin_res_empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSource();
    }

    private void loadSource() {
        if (!live) {
            cachedReservations.clear();
            cachedReservations.addAll(repo.getAllReservations());
            cachedListings.clear();
            cachedListings.addAll(repo.getAll());
            cachedVendors.clear();
            cachedVendors.addAll(repo.getVendors());
            refresh();
            return;
        }
        View overlay = LoadingOverlay.show(this);
        RemoteRepository remote = RepositoryProvider.remote(this);
        remote.getAllReservations(new SupaResult<List<Reservation>>() {
            @Override public void onSuccess(List<Reservation> reservations) {
                remote.getAllListings(new SupaResult<List<FoodListing>>() {
                    @Override public void onSuccess(List<FoodListing> listings) {
                        remote.getVendors(null, new SupaResult<List<Vendor>>() {
                            @Override public void onSuccess(List<Vendor> vendors) {
                                LoadingOverlay.hide(overlay);
                                cachedReservations.clear();
                                cachedReservations.addAll(reservations);
                                cachedListings.clear();
                                cachedListings.addAll(listings);
                                cachedVendors.clear();
                                cachedVendors.addAll(vendors);
                                refresh();
                            }
                            @Override public void onError(String m) {
                                LoadingOverlay.hide(overlay);
                                LoadingOverlay.error(AdminReservationsActivity.this, m,
                                        () -> loadSource());
                            }
                        });
                    }
                    @Override public void onError(String m) {
                        LoadingOverlay.hide(overlay);
                        LoadingOverlay.error(AdminReservationsActivity.this, m,
                                () -> loadSource());
                    }
                });
            }
            @Override public void onError(String m) {
                LoadingOverlay.hide(overlay);
                LoadingOverlay.error(AdminReservationsActivity.this, m, () -> loadSource());
            }
        });
    }

    private void refresh() {
        if (adapter == null) return;
        String q = search.getText() == null ? "" : search.getText().toString();
        java.util.Map<String, FoodListing> byId = new java.util.HashMap<>();
        for (FoodListing l : cachedListings) byId.put(l.getId(), l);
        java.util.Map<String, String> vendorNames = new java.util.HashMap<>();
        for (Vendor v : cachedVendors) vendorNames.put(v.getId(), v.getName());
        List<AdminReservationAdapter.AdminResRow> rows = new ArrayList<>();
        for (Reservation r : cachedReservations) {
            if (filter != null && r.getStatus() != filter) continue;
            FoodListing l = byId.get(r.getListingId());
            String listingName = l == null ? "Listing unavailable" : l.getName();
            String vendorName = l == null ? ""
                    : vendorNames.getOrDefault(l.getVendorId(), "");
            if (!AdminStats.reservationMatches(r, listingName, vendorName, q)) continue;
            double unit = l == null ? 0.0 : l.getDiscountedPrice();
            long deadline = l == null ? 0 : l.getCollectionDeadlineEpoch();
            rows.add(new AdminReservationAdapter.AdminResRow(r, listingName, vendorName,
                    unit, deadline));
        }
        adapter.setRows(rows);
        boolean hasRows = !rows.isEmpty();
        list.setVisibility(hasRows ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasRows ? View.GONE : View.VISIBLE);
    }
}

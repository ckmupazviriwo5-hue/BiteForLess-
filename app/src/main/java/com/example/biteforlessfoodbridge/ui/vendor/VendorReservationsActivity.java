package com.example.biteforlessfoodbridge.ui.vendor;

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
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.ReservationStatus;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/** Reservations on the demo vendor's own listings, with status filtering. */
public class VendorReservationsActivity extends AppCompatActivity
        implements VendorReservationAdapter.Listener {

    private static final String[] FILTERS = {"All", "Reserved", "Completed", "Cancelled", "Expired"};

    private DemoRepository repo;
    private String vendorId;
    private boolean live;
    private VendorReservationAdapter adapter;
    private RecyclerView list;
    private View empty;
    private ReservationStatus filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_reservations);

        repo = DemoRepository.getInstance();
        live = RepositoryProvider.isLive(this);
        if (!live) {
            vendorId = new SessionManager(this).requireDemoVendorId();
        }

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.vendor_res_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ChipGroup group = findViewById(R.id.vendor_res_filter);
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

        list = findViewById(R.id.vendor_res_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VendorReservationAdapter(this);
        list.setAdapter(adapter);
        empty = findViewById(R.id.vendor_res_empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!live) {
            repo.markExpiredReservations(System.currentTimeMillis());
        }
        refresh();
    }

    private void refresh() {
        if (live) {
            refreshLive();
            return;
        }
        List<VendorReservationAdapter.VendorResRow> rows = new ArrayList<>();
        for (Reservation r : repo.getReservationsForVendor(vendorId)) {
            if (filter != null && r.getStatus() != filter) continue;
            FoodListing l = repo.getById(r.getListingId());
            String name = l == null ? "Listing unavailable" : l.getName();
            long deadline = l == null ? 0 : l.getCollectionDeadlineEpoch();
            rows.add(new VendorReservationAdapter.VendorResRow(r, name, deadline));
        }
        showRows(rows);
    }

    private void refreshLive() {
        View overlay = LoadingOverlay.show(this);
        RemoteRepository remote = RepositoryProvider.remote(this);
        remote.getMyVendor(new SupaResult<Vendor>() {
            @Override public void onSuccess(Vendor vendor) {
                vendorId = vendor.getId();
                remote.getVendorReservations(vendorId,
                        new SupaResult<List<Reservation>>() {
                            @Override public void onSuccess(List<Reservation> mine) {
                                remote.getAllListings(new SupaResult<List<FoodListing>>() {
                                    @Override public void onSuccess(List<FoodListing> all) {
                                        LoadingOverlay.hide(overlay);
                                        java.util.Map<String, FoodListing> byId =
                                                new java.util.HashMap<>();
                                        for (FoodListing l : all) byId.put(l.getId(), l);
                                        List<VendorReservationAdapter.VendorResRow> rows =
                                                new ArrayList<>();
                                        for (Reservation r : mine) {
                                            if (filter != null && r.getStatus() != filter) {
                                                continue;
                                            }
                                            FoodListing l = byId.get(r.getListingId());
                                            rows.add(new VendorReservationAdapter.VendorResRow(
                                                    r,
                                                    l == null ? "Listing unavailable" : l.getName(),
                                                    l == null ? 0 : l
                                                            .getCollectionDeadlineEpoch()));
                                        }
                                        showRows(rows);
                                    }
                                    @Override public void onError(String m) {
                                        LoadingOverlay.hide(overlay);
                                        showRows(new ArrayList<>());
                                        LoadingOverlay.error(
                                                VendorReservationsActivity.this, m,
                                                () -> refresh());
                                    }
                                });
                            }
                            @Override public void onError(String m) {
                                LoadingOverlay.hide(overlay);
                                showRows(new ArrayList<>());
                                LoadingOverlay.error(VendorReservationsActivity.this, m,
                                        () -> refresh());
                            }
                        });
            }
            @Override public void onError(String m) {
                LoadingOverlay.hide(overlay);
                showRows(new ArrayList<>());
                LoadingOverlay.error(VendorReservationsActivity.this, m, () -> refresh());
            }
        });
    }

    private void showRows(List<VendorReservationAdapter.VendorResRow> rows) {
        adapter.setRows(rows);
        boolean hasRows = !rows.isEmpty();
        list.setVisibility(hasRows ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasRows ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onComplete(Reservation reservation) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Mark as picked up?")
                .setMessage("Confirm " + (live ? "pickup" : "simulated pickup")
                        + " for " + reservation.getId()
                        + (live ? "?" : " (demo only — no verification)."))
                .setPositiveButton("Confirm pickup", (d, w) -> {
                    if (live) {
                        View overlay = LoadingOverlay.show(this);
                        RepositoryProvider.remote(this).rpcComplete(
                                reservation.getId(), new SupaResult<String>() {
                                    @Override public void onSuccess(String v) {
                                        LoadingOverlay.hide(overlay);
                                        Toast.makeText(VendorReservationsActivity.this,
                                                "Pickup recorded", Toast.LENGTH_SHORT).show();
                                        refresh();
                                    }
                                    @Override public void onError(String m) {
                                        LoadingOverlay.hide(overlay);
                                        Toast.makeText(VendorReservationsActivity.this, m,
                                                Toast.LENGTH_LONG).show();
                                        refresh();
                                    }
                                });
                        return;
                    }
                    try {
                        repo.completeForVendor(reservation.getId(), vendorId);
                        Toast.makeText(this, "Pickup recorded (demo)", Toast.LENGTH_SHORT).show();
                    } catch (IllegalStateException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

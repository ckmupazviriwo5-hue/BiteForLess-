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
            rows.add(new VendorReservationAdapter.VendorResRow(r, name, deadline,
                    r.getUserId()));
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
                                            String name = l == null
                                                    ? (r.getFoodName().isEmpty()
                                                            ? "Listing unavailable"
                                                            : r.getFoodName())
                                                    : l.getName();
                                            rows.add(new VendorReservationAdapter.VendorResRow(
                                                    r,
                                                    name,
                                                    l == null ? 0 : l
                                                            .getCollectionDeadlineEpoch(),
                                                    r.getCustomerName()));
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
        if (live) {
            // The local API verifies pickups with the customer's 4-digit PIN
            // (contract §6) — collect it before confirming.
            final com.google.android.material.textfield.TextInputEditText pinInput =
                    new com.google.android.material.textfield.TextInputEditText(this);
            pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            pinInput.setFilters(new android.text.InputFilter[]
                    { new android.text.InputFilter.LengthFilter(4) });
            pinInput.setHint("4-digit pickup PIN");
            android.widget.LinearLayout wrap = new android.widget.LinearLayout(this);
            wrap.setOrientation(android.widget.LinearLayout.VERTICAL);
            int pad = (int) (16 * getResources().getDisplayMetrics().density);
            wrap.setPadding(pad, pad / 2, pad, 0);
            wrap.addView(pinInput);

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Verify pickup")
                    .setMessage("Ask the customer for the 4-digit PIN shown on their "
                            + "reservation " + (reservation.getPickupCode().isEmpty()
                            ? "" : "(" + reservation.getPickupCode() + ") ")
                            + "and enter it to mark the pickup as collected.")
                    .setView(wrap)
                    .setPositiveButton("Verify pickup", (d, w) -> {
                        String pin = pinInput.getText() == null ? ""
                                : pinInput.getText().toString().trim();
                        if (pin.length() != 4) {
                            Toast.makeText(this, "Enter the 4-digit PIN",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        View overlay = LoadingOverlay.show(this);
                        RepositoryProvider.remote(this).verifyPickup(
                                reservation.getId(), pin, new SupaResult<String>() {
                                    @Override public void onSuccess(String v) {
                                        LoadingOverlay.hide(overlay);
                                        Toast.makeText(VendorReservationsActivity.this,
                                                "Pickup verified", Toast.LENGTH_SHORT).show();
                                        refresh();
                                    }
                                    @Override public void onError(String m) {
                                        LoadingOverlay.hide(overlay);
                                        Toast.makeText(VendorReservationsActivity.this, m,
                                                Toast.LENGTH_LONG).show();
                                        refresh();
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Mark as picked up?")
                .setMessage("Confirm simulated pickup for " + reservation.getId()
                        + " (demo only — no verification).")
                .setPositiveButton("Confirm pickup", (d, w) -> {
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

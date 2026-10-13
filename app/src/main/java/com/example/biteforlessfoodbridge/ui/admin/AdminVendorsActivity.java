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
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.model.VendorStatus;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/** Vendor directory: approve / suspend / reactivate with confirmations. */
public class AdminVendorsActivity extends AppCompatActivity implements AdminVendorAdapter.Listener {

    private static final String[] FILTERS = {"All", "Approved", "Pending", "Suspended"};

    private DemoRepository repo;
    private String actor;
    private boolean live;
    private final List<Vendor> cachedVendors = new ArrayList<>();
    private final List<FoodListing> cachedListings = new ArrayList<>();
    private AdminVendorAdapter adapter;
    private RecyclerView list;
    private View empty;
    private VendorStatus filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_vendors);

        repo = DemoRepository.getInstance();
        live = RepositoryProvider.isLive(this);
        SessionManager session = new SessionManager(this);
        String email = session.getEmail();
        actor = email == null || email.isEmpty() ? "admin (demo)" : email;

        MaterialToolbar toolbar = findViewById(R.id.admin_vendors_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ChipGroup group = findViewById(R.id.admin_vendors_filter);
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
                if ("Approved".equals(label)) filter = VendorStatus.APPROVED;
                else if ("Pending".equals(label)) filter = VendorStatus.PENDING;
                else if ("Suspended".equals(label)) filter = VendorStatus.SUSPENDED;
                else filter = null;
            }
            refresh();
        });

        list = findViewById(R.id.admin_vendors_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminVendorAdapter(this);
        list.setAdapter(adapter);
        empty = findViewById(R.id.admin_vendors_empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (live) {
            refreshLive();
            return;
        }
        cachedVendors.clear();
        cachedVendors.addAll(repo.getVendors());
        showVendors(cachedVendors);
    }

    private void refreshLive() {
        View overlay = LoadingOverlay.show(this);
        RemoteRepository remote = RepositoryProvider.remote(this);
        remote.getVendors(null, new SupaResult<List<Vendor>>() {
            @Override public void onSuccess(List<Vendor> vendors) {
                remote.getAllListings(new SupaResult<List<FoodListing>>() {
                    @Override public void onSuccess(List<FoodListing> listings) {
                        LoadingOverlay.hide(overlay);
                        cachedVendors.clear();
                        cachedVendors.addAll(vendors);
                        cachedListings.clear();
                        cachedListings.addAll(listings);
                        showVendors(vendors);
                    }
                    @Override public void onError(String m) {
                        LoadingOverlay.hide(overlay);
                        showVendors(new ArrayList<>());
                        LoadingOverlay.error(AdminVendorsActivity.this, m,
                                () -> refresh());
                    }
                });
            }
            @Override public void onError(String m) {
                LoadingOverlay.hide(overlay);
                showVendors(new ArrayList<>());
                LoadingOverlay.error(AdminVendorsActivity.this, m, () -> refresh());
            }
        });
    }

    private void showVendors(List<Vendor> vendors) {
        List<AdminVendorAdapter.VendorRow> rows = new ArrayList<>();
        for (Vendor v : vendors) {
            if (filter != null && v.getStatus() != filter) continue;
            int count = 0;
            if (live) {
                for (FoodListing l : cachedListings) {
                    if (v.getId().equals(l.getVendorId())) count++;
                }
            } else {
                count = repo.getListingCountForVendor(v.getId());
            }
            rows.add(new AdminVendorAdapter.VendorRow(v, count));
        }
        adapter.setRows(rows);
        boolean hasRows = !rows.isEmpty();
        list.setVisibility(hasRows ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasRows ? View.GONE : View.VISIBLE);
    }

    private List<FoodListing> ownedListings(String vendorId) {
        List<FoodListing> out = new ArrayList<>();
        List<FoodListing> source = live ? cachedListings : repo.getListingsForVendor(vendorId);
        for (FoodListing l : source) {
            if (live && !vendorId.equals(l.getVendorId())) continue;
            out.add(l);
        }
        return out;
    }

    @Override
    public void onDetails(Vendor vendor) {
        StringBuilder sb = new StringBuilder();
        sb.append(vendor.getLocation()).append("\nStatus: ").append(vendor.getStatus().name());
        sb.append("\n\nListings:");
        List<FoodListing> owned = ownedListings(vendor.getId());
        if (owned.isEmpty()) {
            sb.append(" none");
        } else {
            for (FoodListing l : owned) {
                sb.append("\n• ").append(l.getName())
                        .append(" (").append(l.getStatus().name())
                        .append(", ").append(l.getAvailableQuantity()).append(" left)");
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(vendor.getName())
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public void onAction(Vendor vendor) {
        VendorStatus target;
        String verb;
        String extra = "";
        if (vendor.getStatus() == VendorStatus.PENDING) {
            target = VendorStatus.APPROVED;
            verb = "approve";
        } else if (vendor.getStatus() == VendorStatus.APPROVED) {
            target = VendorStatus.SUSPENDED;
            verb = "suspend";
            extra = " Their listings stay visible but cannot take new orders;"
                    + " existing reservations are preserved.";
        } else {
            target = VendorStatus.APPROVED;
            verb = "reactivate";
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm")
                .setMessage("Do you want to " + verb + " \"" + vendor.getName() + "\"?" + extra)
                .setPositiveButton("Confirm", (d, w) -> {
                    if (live) {
                        View overlay = LoadingOverlay.show(this);
                        RepositoryProvider.remote(this).rpcSetVendorStatus(
                                vendor.getId(), target, new SupaResult<String>() {
                                    @Override public void onSuccess(String v) {
                                        LoadingOverlay.hide(overlay);
                                        Toast.makeText(AdminVendorsActivity.this,
                                                "Vendor " + verb + "d",
                                                Toast.LENGTH_SHORT).show();
                                        refresh();
                                    }
                                    @Override public void onError(String m) {
                                        LoadingOverlay.hide(overlay);
                                        Toast.makeText(AdminVendorsActivity.this, m,
                                                Toast.LENGTH_LONG).show();
                                        refresh();
                                    }
                                });
                        return;
                    }
                    try {
                        repo.setVendorStatus(vendor.getId(), target, actor);
                        Toast.makeText(this, "Vendor " + verb + "d (demo)",
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

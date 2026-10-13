package com.example.biteforlessfoodbridge.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.example.biteforlessfoodbridge.model.Category;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.example.biteforlessfoodbridge.ui.customer.DealDetailActivity;
import com.example.biteforlessfoodbridge.util.ListingQuery;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** All-platform listing directory with safe deactivate/reactivate. */
public class AdminListingsActivity extends AppCompatActivity implements AdminListingAdapter.Listener {

    private static final String[] STATUS_LABELS = {"All", "Bookable", "Paused", "Sold out", "Expired"};
    private static final ListingQuery.StatusFilter[] STATUS_VALUES = {
            ListingQuery.StatusFilter.ALL, ListingQuery.StatusFilter.BOOKABLE,
            ListingQuery.StatusFilter.PAUSED, ListingQuery.StatusFilter.SOLD_OUT,
            ListingQuery.StatusFilter.EXPIRED};

    private DemoRepository repo;
    private String actor;
    private boolean live;
    private final List<FoodListing> cachedListings = new ArrayList<>();
    private final List<Vendor> cachedVendors = new ArrayList<>();
    private final List<Category> cachedCategories = new ArrayList<>();
    private AdminListingAdapter adapter;
    private TextInputEditText search;
    private ChipGroup statusGroup;
    private Spinner categorySpinner;
    private TextView count;
    private RecyclerView list;
    private View empty;

    private ListingQuery.StatusFilter statusFilter = ListingQuery.StatusFilter.ALL;
    private String categoryId = "all";
    private List<String> categoryIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_listings);

        repo = DemoRepository.getInstance();
        live = RepositoryProvider.isLive(this);
        SessionManager session = new SessionManager(this);
        String email = session.getEmail();
        actor = email == null || email.isEmpty() ? "admin (demo)" : email;

        MaterialToolbar toolbar = findViewById(R.id.admin_listings_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        search = findViewById(R.id.admin_listings_search);
        statusGroup = findViewById(R.id.admin_listings_status);
        categorySpinner = findViewById(R.id.admin_listings_category);
        count = findViewById(R.id.admin_listings_count);
        list = findViewById(R.id.admin_listings_list);
        empty = findViewById(R.id.admin_listings_empty);

        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminListingAdapter(this);
        list.setAdapter(adapter);

        for (int i = 0; i < STATUS_LABELS.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(STATUS_LABELS[i]);
            chip.setTag(STATUS_VALUES[i]);
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            statusGroup.addView(chip);
        }
        statusGroup.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                statusFilter = ListingQuery.StatusFilter.ALL;
            } else {
                Chip chip = g.findViewById(checkedIds.get(0));
                statusFilter = chip == null || chip.getTag() == null
                        ? ListingQuery.StatusFilter.ALL
                        : (ListingQuery.StatusFilter) chip.getTag();
            }
            refresh();
        });

        categoryIds.add("all");
        List<String> names = new ArrayList<>();
        names.add("All categories");
        if (!live) {
            for (Category c : repo.getCategories()) {
                categoryIds.add(c.getId());
                names.add(c.getName());
            }
        }
        setCategoryAdapter(names);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                categoryId = categoryIds.get(Math.min(pos, categoryIds.size() - 1));
                refresh();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { refresh(); }
        });
    }

    private void setCategoryAdapter(List<String> names) {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(catAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSource();
    }

    private void loadSource() {
        if (!live) {
            cachedListings.clear();
            cachedListings.addAll(repo.getAll());
            cachedVendors.clear();
            cachedVendors.addAll(repo.getVendors());
            cachedCategories.clear();
            cachedCategories.addAll(repo.getCategories());
            refresh();
            return;
        }
        View overlay = LoadingOverlay.show(this);
        RemoteRepository remote = RepositoryProvider.remote(this);
        remote.getAllListings(new SupaResult<List<FoodListing>>() {
            @Override public void onSuccess(List<FoodListing> listings) {
                remote.getVendors(null, new SupaResult<List<Vendor>>() {
                    @Override public void onSuccess(List<Vendor> vendors) {
                        remote.getCategories(new SupaResult<List<Category>>() {
                            @Override public void onSuccess(List<Category> cats) {
                                LoadingOverlay.hide(overlay);
                                cachedListings.clear();
                                cachedListings.addAll(listings);
                                cachedVendors.clear();
                                cachedVendors.addAll(vendors);
                                cachedCategories.clear();
                                cachedCategories.addAll(cats);
                                categoryIds.clear();
                                categoryIds.add("all");
                                List<String> names = new ArrayList<>();
                                names.add("All categories");
                                for (Category c : cats) {
                                    categoryIds.add(c.getId());
                                    names.add(c.getName());
                                }
                                categoryId = "all";
                                setCategoryAdapter(names);
                                refresh();
                            }
                            @Override public void onError(String m) {
                                LoadingOverlay.hide(overlay);
                                LoadingOverlay.error(AdminListingsActivity.this, m,
                                        () -> loadSource());
                            }
                        });
                    }
                    @Override public void onError(String m) {
                        LoadingOverlay.hide(overlay);
                        LoadingOverlay.error(AdminListingsActivity.this, m,
                                () -> loadSource());
                    }
                });
            }
            @Override public void onError(String m) {
                LoadingOverlay.hide(overlay);
                LoadingOverlay.error(AdminListingsActivity.this, m, () -> loadSource());
            }
        });
    }

    private void refresh() {
        if (adapter == null) return;
        String q = search.getText() == null ? "" : search.getText().toString();
        Map<String, Vendor> vendorMap = new HashMap<>();
        Map<String, String> vendorNames = new HashMap<>();
        for (Vendor v : cachedVendors) {
            vendorMap.put(v.getId(), v);
            vendorNames.put(v.getId(), v.getName());
        }
        List<FoodListing> result = ListingQuery.applyForAdmin(cachedListings, vendorMap, q,
                categoryId, statusFilter, ListingQuery.SortOrder.DEFAULT,
                System.currentTimeMillis());
        adapter.setRows(result, vendorNames);
        boolean hasRows = !result.isEmpty();
        list.setVisibility(hasRows ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasRows ? View.GONE : View.VISIBLE);
        count.setText(result.size() == 1 ? "1 listing" : result.size() + " listings");
    }

    @Override
    public void onView(FoodListing listing) {
        Intent intent = new Intent(this, DealDetailActivity.class);
        intent.putExtra(DealDetailActivity.EXTRA_LISTING_ID, listing.getId());
        startActivity(intent);
    }

    @Override
    public void onToggleStatus(FoodListing listing) {
        boolean activating = listing.getStatus() != ListingStatus.ACTIVE;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm")
                .setMessage("Do you want to " + (activating ? "reactivate" : "deactivate")
                        + " \"" + listing.getName() + "\"?"
                        + (activating ? " Reservability still depends on vendor approval,"
                        + " stock and deadline." : " Customers will no longer be able to"
                        + " reserve it; reservation history is preserved."))
                .setPositiveButton("Confirm", (d, w) -> {
                    if (live) {
                        View overlay = LoadingOverlay.show(this);
                        RepositoryProvider.remote(this).rpcSetListingStatus(
                                listing.getId(),
                                activating ? ListingStatus.ACTIVE : ListingStatus.INACTIVE,
                                new SupaResult<String>() {
                                    @Override public void onSuccess(String v) {
                                        LoadingOverlay.hide(overlay);
                                        Toast.makeText(AdminListingsActivity.this,
                                                "Listing updated", Toast.LENGTH_SHORT).show();
                                        loadSource();
                                    }
                                    @Override public void onError(String m) {
                                        LoadingOverlay.hide(overlay);
                                        Toast.makeText(AdminListingsActivity.this, m,
                                                Toast.LENGTH_LONG).show();
                                        loadSource();
                                    }
                                });
                        return;
                    }
                    try {
                        repo.setListingStatusByAdmin(listing.getId(),
                                activating ? ListingStatus.ACTIVE : ListingStatus.INACTIVE,
                                actor);
                        Toast.makeText(this, "Listing updated (demo)", Toast.LENGTH_SHORT).show();
                    } catch (IllegalStateException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

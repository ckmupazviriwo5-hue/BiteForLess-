package com.example.biteforlessfoodbridge.ui.customer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.biteforlessfoodbridge.MainActivity;
import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.data.RepositoryProvider;
import com.example.biteforlessfoodbridge.data.remote.RemoteRepository;
import com.example.biteforlessfoodbridge.data.remote.SupaResult;
import com.example.biteforlessfoodbridge.model.Category;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.example.biteforlessfoodbridge.util.ListingQuery;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Browse + search + filter + sort. Source is the live backend when signed in,
 * otherwise the shared demo repository (labelled in the count line).
 */
public class DealsFragment extends Fragment implements DealAdapter.OnDealClick {

    public static final String ARG_CATEGORY = "arg_category";

    private static final String[] DISCOUNT_OPTIONS = {"Any discount", "20%+", "30%+", "50%+"};
    private static final int[] DISCOUNT_VALUES = {0, 20, 30, 50};
    private static final String[] SORT_OPTIONS =
            {"Recommended", "Price: low to high", "Price: high to low", "Biggest discount", "Ending soon"};

    private final List<FoodListing> allListings = new ArrayList<>();
    private final Map<String, Vendor> vendorMap = new HashMap<>();
    private final Map<String, String> vendorNames = new HashMap<>();
    private final Map<String, String> categoryNames = new HashMap<>();
    private final List<Category> categories = new ArrayList<>();
    private boolean live;
    private boolean sourceLoaded;

    private DealAdapter adapter;
    private TextInputEditText search;
    private ChipGroup categoryChips;
    private Spinner sort;
    private Spinner minDiscount;
    private TextInputEditText maxPrice;
    private MaterialCheckBox availableOnly;
    private TextView count;
    private RecyclerView list;
    private View empty;

    private String selectedCategory = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_deals, container, false);
        live = RepositoryProvider.isLive(requireContext());

        search = root.findViewById(R.id.deals_search);
        categoryChips = root.findViewById(R.id.deals_categories);
        sort = root.findViewById(R.id.deals_sort);
        minDiscount = root.findViewById(R.id.deals_min_discount);
        maxPrice = root.findViewById(R.id.deals_max_price);
        availableOnly = root.findViewById(R.id.deals_available_only);
        count = root.findViewById(R.id.deals_count);
        list = root.findViewById(R.id.deals_list);
        empty = root.findViewById(R.id.deals_empty);
        Button clear = root.findViewById(R.id.deals_clear);
        Button emptyClear = root.findViewById(R.id.deals_empty_clear);

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DealAdapter(this);
        list.setAdapter(adapter);

        sort.setAdapter(simpleAdapter(SORT_OPTIONS));
        minDiscount.setAdapter(simpleAdapter(DISCOUNT_OPTIONS));

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { refresh(); }
        };
        search.addTextChangedListener(watcher);
        maxPrice.addTextChangedListener(watcher);
        availableOnly.setOnCheckedChangeListener((b, checked) -> refresh());
        sort.setOnItemSelectedListener(selected(() -> refresh()));
        minDiscount.setOnItemSelectedListener(selected(() -> refresh()));
        clear.setOnClickListener(v -> clearFilters());
        emptyClear.setOnClickListener(v -> clearFilters());

        String preset = null;
        if (getArguments() != null) preset = getArguments().getString(ARG_CATEGORY);
        if (preset == null && requireActivity() instanceof MainActivity) {
            preset = ((MainActivity) requireActivity()).consumePendingCategory();
        }
        if (preset != null) selectedCategory = preset;

        loadSource();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sourceLoaded) {
            if (live) loadSource(); // fresh stock from server
            else {
                cacheDemo();
                refresh();
            }
        }
    }

    /** Loads listings/vendors/categories from live backend or demo repo. */
    private void loadSource() {
        if (!live) {
            cacheDemo();
            buildCategoryChips();
            sourceLoaded = true;
            refresh();
            return;
        }
        Activity activity = getActivity();
        if (activity == null) return;
        View overlay = LoadingOverlay.show(activity);
        RemoteRepository remote = RepositoryProvider.remote(requireContext());
        remote.getStorefrontListings(new SupaResult<List<FoodListing>>() {
            @Override public void onSuccess(List<FoodListing> listings) {
                remote.getVendors(null, new SupaResult<List<Vendor>>() {
                    @Override public void onSuccess(List<Vendor> vendors) {
                        remote.getCategories(new SupaResult<List<Category>>() {
                            @Override public void onSuccess(List<Category> cats) {
                                LoadingOverlay.hide(overlay);
                                cacheRemote(listings, vendors, cats);
                                buildCategoryChips();
                                sourceLoaded = true;
                                refresh();
                            }
                            @Override public void onError(String m) {
                                LoadingOverlay.hide(overlay);
                                loadFailed(m);
                            }
                        });
                    }
                    @Override public void onError(String m) {
                        LoadingOverlay.hide(overlay);
                        loadFailed(m);
                    }
                });
            }
            @Override public void onError(String m) {
                LoadingOverlay.hide(overlay);
                loadFailed(m);
            }
        });
    }

    private void loadFailed(String message) {
        Activity activity = getActivity();
        if (activity == null) return;
        sourceLoaded = true;
        allListings.clear();
        refresh();
        LoadingOverlay.error(activity, message, this::loadSource);
    }

    private void cacheDemo() {
        DemoRepository repo = RepositoryProvider.demo();
        allListings.clear();
        allListings.addAll(repo.getAll());
        vendorMap.clear();
        vendorNames.clear();
        for (Vendor v : repo.getVendors()) {
            vendorMap.put(v.getId(), v);
            vendorNames.put(v.getId(), v.getName());
        }
        categories.clear();
        categories.addAll(repo.getCategories());
        categoryNames.clear();
        for (Category c : categories) categoryNames.put(c.getId(), c.getName());
    }

    private void cacheRemote(List<FoodListing> listings, List<Vendor> vendors,
                             List<Category> cats) {
        allListings.clear();
        if (listings != null) allListings.addAll(listings);
        vendorMap.clear();
        vendorNames.clear();
        if (vendors != null) {
            for (Vendor v : vendors) {
                vendorMap.put(v.getId(), v);
                vendorNames.put(v.getId(), v.getName());
            }
        }
        categories.clear();
        if (cats != null) categories.addAll(cats);
        categoryNames.clear();
        for (Category c : categories) categoryNames.put(c.getId(), c.getName());
    }

    private ArrayAdapter<String> simpleAdapter(String[] items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private AdapterView.OnItemSelectedListener selected(Runnable r) {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { r.run(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
    }

    private void buildCategoryChips() {
        categoryChips.removeAllViews();
        addChip("all", "All");
        for (Category c : categories) {
            addChip(c.getId(), c.getName());
        }
        selectCategory(selectedCategory);
        categoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedCategory = "all";
            } else {
                Chip chip = group.findViewById(checkedIds.get(0));
                selectedCategory = chip == null || chip.getTag() == null
                        ? "all" : chip.getTag().toString();
            }
            refresh();
        });
    }

    private void addChip(String id, String label) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setTag(id);
        chip.setCheckable(true);
        chip.setChecked("all".equals(id) && "all".equals(selectedCategory));
        categoryChips.addView(chip);
    }

    private void selectCategory(String categoryId) {
        selectedCategory = categoryId == null ? "all" : categoryId;
        for (int i = 0; i < categoryChips.getChildCount(); i++) {
            View child = categoryChips.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChecked(selectedCategory.equals(chip.getTag()));
            }
        }
    }

    private void clearFilters() {
        search.setText("");
        maxPrice.setText("");
        availableOnly.setChecked(false);
        sort.setSelection(0);
        minDiscount.setSelection(0);
        selectCategory("all");
        refresh();
    }

    private void refresh() {
        if (adapter == null) return;
        String q = search.getText() == null ? "" : search.getText().toString();
        String maxRaw = maxPrice.getText() == null ? "" : maxPrice.getText().toString().trim();
        Double max = null;
        if (!maxRaw.isEmpty()) {
            try {
                max = Double.parseDouble(maxRaw);
            } catch (NumberFormatException ignored) {
            }
        }
        int discountPos = Math.max(0, minDiscount.getSelectedItemPosition());
        int minDisc = DISCOUNT_VALUES[Math.min(discountPos, DISCOUNT_VALUES.length - 1)];

        ListingQuery.SortOrder order;
        switch (sort.getSelectedItemPosition()) {
            case 1: order = ListingQuery.SortOrder.PRICE_ASC; break;
            case 2: order = ListingQuery.SortOrder.PRICE_DESC; break;
            case 3: order = ListingQuery.SortOrder.DISCOUNT_DESC; break;
            case 4: order = ListingQuery.SortOrder.DEADLINE_ASC; break;
            default: order = ListingQuery.SortOrder.DEFAULT; break;
        }

        List<FoodListing> result = ListingQuery.apply(allListings, vendorMap, q,
                selectedCategory, max, minDisc, availableOnly.isChecked(),
                order, System.currentTimeMillis());

        List<DealAdapter.DealRow> rows = new ArrayList<>();
        for (FoodListing l : result) {
            String vn = vendorNames.get(l.getVendorId());
            String cn = categoryNames.get(l.getCategoryId());
            rows.add(new DealAdapter.DealRow(l,
                    vn == null ? "Unknown vendor" : vn,
                    cn == null ? l.getCategoryId() : cn));
        }
        adapter.setRows(rows);

        boolean hasResults = !rows.isEmpty();
        list.setVisibility(hasResults ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasResults ? View.GONE : View.VISIBLE);
        String base = rows.size() == 1 ? "1 deal" : rows.size() + " deals";
        count.setText(live ? base + " · live" : base + " · demo");
    }

    @Override
    public void onDealClick(String listingId) {
        Intent intent = new Intent(requireContext(), DealDetailActivity.class);
        intent.putExtra(DealDetailActivity.EXTRA_LISTING_ID, listingId);
        startActivity(intent);
    }
}

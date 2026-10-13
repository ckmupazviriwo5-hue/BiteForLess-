package com.example.biteforlessfoodbridge.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.example.biteforlessfoodbridge.data.SessionManager;
import com.example.biteforlessfoodbridge.data.remote.RemoteRepository;
import com.example.biteforlessfoodbridge.data.remote.SupaResult;
import com.example.biteforlessfoodbridge.model.Category;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.example.biteforlessfoodbridge.util.ListingQuery;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Home: greeting, featured deals, category shortcuts, route to all deals. */
public class HomeFragment extends Fragment implements DealAdapter.OnDealClick {

    private DemoRepository repo;
    private SessionManager session;
    private DealAdapter adapter;
    private RecyclerView featured;
    private View empty;
    private ChipGroup chips;
    private boolean live;
    private boolean loaded;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        repo = DemoRepository.getInstance();
        session = new SessionManager(requireContext());
        live = RepositoryProvider.isLive(requireContext());

        TextView greeting = root.findViewById(R.id.home_greeting);
        String name = session.getName();
        greeting.setText(name == null || name.isEmpty()
                ? "Rescue surplus food near you"
                : "Hi " + firstName(name) + " — rescue surplus food near you");

        featured = root.findViewById(R.id.home_featured);
        featured.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DealAdapter(this);
        featured.setAdapter(adapter);
        empty = root.findViewById(R.id.home_empty);

        ChipGroup chips = root.findViewById(R.id.home_categories);
        this.chips = chips;
        chips.removeAllViews();

        Button browseAll = root.findViewById(R.id.home_browse_all);
        browseAll.setOnClickListener(v -> openCategory("all"));

        loadSource();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (loaded) loadSource();
    }

    private void loadSource() {
        if (!live) {
            buildChips(repo.getCategories());
            refreshDemo();
            loaded = true;
            return;
        }
        if (getActivity() == null) return;
        View overlay = LoadingOverlay.show(getActivity());
        RemoteRepository remote = RepositoryProvider.remote(requireContext());
        remote.getStorefrontListings(new SupaResult<List<FoodListing>>() {
            @Override public void onSuccess(List<FoodListing> listings) {
                remote.getVendors(null, new SupaResult<List<Vendor>>() {
                    @Override public void onSuccess(List<Vendor> vendors) {
                        remote.getCategories(new SupaResult<List<Category>>() {
                            @Override public void onSuccess(List<Category> cats) {
                                LoadingOverlay.hide(overlay);
                                buildChips(cats);
                                refreshRemote(listings, vendors, cats);
                                loaded = true;
                            }
                            @Override public void onError(String m) {
                                LoadingOverlay.hide(overlay);
                                failed(m);
                            }
                        });
                    }
                    @Override public void onError(String m) {
                        LoadingOverlay.hide(overlay);
                        failed(m);
                    }
                });
            }
            @Override public void onError(String m) {
                LoadingOverlay.hide(overlay);
                failed(m);
            }
        });
    }

    private void failed(String message) {
        loaded = true;
        adapter.setRows(new ArrayList<>());
        featured.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
        if (getActivity() != null) {
            LoadingOverlay.error(getActivity(), message, () -> loadSource());
        }
    }

    private void buildChips(List<Category> cats) {
        chips.removeAllViews();
        for (Category c : cats) {
            Chip chip = new Chip(requireContext());
            chip.setText(c.getName());
            chip.setCheckable(false);
            chip.setOnClickListener(v -> openCategory(c.getId()));
            chips.addView(chip);
        }
    }

    private void refreshDemo() {
        if (adapter == null) return;
        Map<String, Vendor> vendorMap = new HashMap<>();
        for (Vendor v : repo.getVendors()) vendorMap.put(v.getId(), v);
        List<FoodListing> available = ListingQuery.apply(repo.getAll(), vendorMap, "",
                "all", null, 0, true, ListingQuery.SortOrder.DISCOUNT_DESC,
                System.currentTimeMillis());
        showTop(available, vendorMap, null);
    }

    private void refreshRemote(List<FoodListing> listings, List<Vendor> vendors,
                               List<Category> cats) {
        if (adapter == null) return;
        Map<String, Vendor> vendorMap = new HashMap<>();
        for (Vendor v : vendors) vendorMap.put(v.getId(), v);
        Map<String, String> catNames = new HashMap<>();
        for (Category c : cats) catNames.put(c.getId(), c.getName());
        List<FoodListing> available = ListingQuery.apply(listings, vendorMap, "",
                "all", null, 0, true, ListingQuery.SortOrder.DISCOUNT_DESC,
                System.currentTimeMillis());
        showTop(available, vendorMap, catNames);
    }

    private void showTop(List<FoodListing> available, Map<String, Vendor> vendorMap,
                         Map<String, String> catNames) {
        List<FoodListing> top = available.size() > 3 ? available.subList(0, 3) : available;
        Map<String, String> vendorNames = new HashMap<>();
        for (Map.Entry<String, Vendor> e : vendorMap.entrySet()) {
            vendorNames.put(e.getKey(), e.getValue().getName());
        }
        List<DealAdapter.DealRow> rows = new ArrayList<>();
        for (FoodListing l : top) {
            String vn = vendorNames.get(l.getVendorId());
            String cn = catNames == null ? repo.getCategoryName(l.getCategoryId())
                    : catNames.getOrDefault(l.getCategoryId(), l.getCategoryId());
            rows.add(new DealAdapter.DealRow(l,
                    vn == null ? "Unknown vendor" : vn, cn));
        }
        adapter.setRows(rows);
        boolean hasRows = !rows.isEmpty();
        featured.setVisibility(hasRows ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasRows ? View.GONE : View.VISIBLE);
    }

    private void refresh() {
        refreshDemo();
    }

    private String firstName(String full) {
        String t = full.trim();
        int space = t.indexOf(' ');
        return space > 0 ? t.substring(0, space) : t;
    }

    private void openCategory(String categoryId) {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openDealsWithCategory(categoryId);
        }
    }

    @Override
    public void onDealClick(String listingId) {
        Intent intent = new Intent(requireContext(), DealDetailActivity.class);
        intent.putExtra(DealDetailActivity.EXTRA_LISTING_ID, listingId);
        startActivity(intent);
    }
}

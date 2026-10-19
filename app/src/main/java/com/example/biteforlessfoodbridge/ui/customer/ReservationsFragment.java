package com.example.biteforlessfoodbridge.ui.customer;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/** Customer reservation history (live backend when signed in, else demo). */
public class ReservationsFragment extends Fragment implements ReservationAdapter.OnCancelClick {

    private DemoRepository repo;
    private SessionManager session;
    private ReservationAdapter adapter;
    private RecyclerView list;
    private View empty;
    private boolean live;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_reservations, container, false);
        repo = DemoRepository.getInstance();
        session = new SessionManager(requireContext());
        live = RepositoryProvider.isLive(requireContext());

        TextView sectionTitle = root.findViewById(R.id.section_title);
        sectionTitle.setText("My reservations");

        list = root.findViewById(R.id.reservations_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReservationAdapter(this);
        list.setAdapter(adapter);

        empty = root.findViewById(R.id.empty_state);
        TextView emptyTitle = root.findViewById(R.id.empty_title);
        emptyTitle.setText("No reservations yet");
        TextView emptySubtitle = root.findViewById(R.id.empty_subtitle);
        emptySubtitle.setText(live
                ? "Reserved pickups appear here with their collection code."
                : "Your demo pickups will appear here.");
        Button emptyAction = root.findViewById(R.id.empty_action);
        emptyAction.setVisibility(View.VISIBLE);
        emptyAction.setText("Browse deals");
        emptyAction.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openDealsWithCategory("all");
            }
        });

        refresh();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (repo == null) return;
        if (!live) {
            repo.markExpiredReservations(System.currentTimeMillis());
        }
        refresh();
    }

    private void refresh() {
        if (adapter == null) return;
        if (live) {
            refreshLive();
            return;
        }
        String userId = session.getEmail();
        List<Reservation> mine = repo.getForUser(userId);
        List<ReservationAdapter.ReservationRow> rows = new ArrayList<>();
        for (Reservation r : mine) {
            FoodListing l = repo.getById(r.getListingId());
            String listingName = l == null ? "Listing unavailable" : l.getName();
            String vendorName = l == null ? "" : repo.getVendorName(l.getVendorId());
            double unit = l == null ? 0.0 : l.getDiscountedPrice();
            long deadline = l == null ? 0 : l.getCollectionDeadlineEpoch();
            String location = l == null ? "" : l.getLocation();
            rows.add(new ReservationAdapter.ReservationRow(r, listingName, vendorName,
                    unit, deadline, location));
        }
        showRows(rows);
    }

    /** Live history: joins reservations with the listing catalog. */
    private void refreshLive() {
        if (getActivity() == null) return;
        View overlay = LoadingOverlay.show(getActivity());
        RemoteRepository remote = RepositoryProvider.remote(requireContext());
        remote.getMyReservations(new SupaResult<List<Reservation>>() {
            @Override public void onSuccess(List<Reservation> mine) {
                remote.getAllListings(new SupaResult<List<FoodListing>>() {
                    @Override public void onSuccess(List<FoodListing> all) {
                        LoadingOverlay.hide(overlay);
                        Map<String, FoodListing> byId = new HashMap<>();
                        for (FoodListing l : all) byId.put(l.getId(), l);
                        remote.getVendors(null, new SupaResult<List<Vendor>>() {
                            @Override public void onSuccess(List<Vendor> vendors) {
                                Map<String, String> names = new HashMap<>();
                                for (Vendor v : vendors) names.put(v.getId(), v.getName());
                                List<ReservationAdapter.ReservationRow> rows = new ArrayList<>();
                                for (Reservation r : mine) {
                                    FoodListing l = byId.get(r.getListingId());
                                    // Reservations on expired/sold listings are no
                                    // longer in the catalog — the reservation row's
                                    // own names keep the history readable.
                                    String ln = l == null
                                            ? (r.getFoodName().isEmpty()
                                                    ? "Listing unavailable" : r.getFoodName())
                                            : l.getName();
                                    String vn = l == null ? r.getShopName()
                                            : names.getOrDefault(l.getVendorId(),
                                                    r.getShopName());
                                    rows.add(new ReservationAdapter.ReservationRow(r, ln, vn,
                                            l == null ? 0.0 : l.getDiscountedPrice(),
                                            l == null ? 0 : l.getCollectionDeadlineEpoch(),
                                            l == null ? "" : l.getLocation()));
                                }
                                showRows(rows);
                            }
                            @Override public void onError(String m) {
                                LoadingOverlay.hide(overlay);
                                showRows(new ArrayList<>());
                                LoadingOverlay.error(getActivity(), m, () -> refresh());
                            }
                        });
                    }
                    @Override public void onError(String m) {
                        LoadingOverlay.hide(overlay);
                        showRows(new ArrayList<>());
                        LoadingOverlay.error(getActivity(), m, () -> refresh());
                    }
                });
            }
            @Override public void onError(String m) {
                LoadingOverlay.hide(overlay);
                showRows(new ArrayList<>());
                LoadingOverlay.error(getActivity(), m, () -> refresh());
            }
        });
    }

    private void showRows(List<ReservationAdapter.ReservationRow> rows) {
        adapter.setRows(rows);
        boolean hasRows = !rows.isEmpty();
        list.setVisibility(hasRows ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasRows ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onCancelClick(Reservation reservation) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancel reservation?")
                .setMessage("Cancel " + reservation.getId() + "? The stock will be released"
                        + (live ? "." : " (demo)."))
                .setPositiveButton("Cancel reservation", (d, w) -> {
                    if (live) {
                        View overlay = LoadingOverlay.show(getActivity());
                        RepositoryProvider.remote(requireContext()).rpcCancel(
                                reservation.getId(), new SupaResult<String>() {
                                    @Override public void onSuccess(String v) {
                                        LoadingOverlay.hide(overlay);
                                        Toast.makeText(requireContext(),
                                                "Reservation cancelled",
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
                        repo.cancel(reservation.getId());
                        Toast.makeText(requireContext(),
                                "Reservation cancelled (demo)", Toast.LENGTH_SHORT).show();
                    } catch (IllegalStateException e) {
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    refresh();
                })
                .setNegativeButton("Keep", null)
                .show();
    }
}

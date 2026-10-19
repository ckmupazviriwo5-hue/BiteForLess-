package com.example.biteforlessfoodbridge.ui.admin;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.data.RepositoryProvider;
import com.example.biteforlessfoodbridge.data.remote.ExpressRepository;
import com.example.biteforlessfoodbridge.data.remote.RemoteRepository;
import com.example.biteforlessfoodbridge.data.remote.SupaResult;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.example.biteforlessfoodbridge.util.AdminStats;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

/** Observed-activity customer directory (not a registration database). */
public class AdminCustomersActivity extends AppCompatActivity {

    private DemoRepository repo;
    private boolean live;
    private AdminCustomerAdapter adapter;
    private RecyclerView list;
    private View empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_customers);

        repo = DemoRepository.getInstance();
        live = RepositoryProvider.isLive(this);

        MaterialToolbar toolbar = findViewById(R.id.admin_customers_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        list = findViewById(R.id.admin_customers_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminCustomerAdapter();
        list.setAdapter(adapter);
        empty = findViewById(R.id.admin_customers_empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (adapter == null) return;
        if (live) {
            View overlay = LoadingOverlay.show(this);
            RemoteRepository remote = RepositoryProvider.remote(this);
            remote.getAllReservations(
                    new SupaResult<java.util.List<Reservation>>() {
                        @Override public void onSuccess(java.util.List<Reservation> all) {
                            List<AdminStats.CustomerSummary> activity =
                                    AdminStats.customerSummaries(all);
                            // Registered-customer directory (GET /api/admin/users),
                            // joined with the reservation-derived activity.
                            if (remote instanceof ExpressRepository) {
                                ((ExpressRepository) remote).getAdminUserLabels(
                                        new SupaResult<java.util.Map<String, String>>() {
                                            @Override public void onSuccess(
                                                    java.util.Map<String, String> labels) {
                                                LoadingOverlay.hide(overlay);
                                                showRows(joinDirectory(labels, activity));
                                            }
                                            @Override public void onError(String m) {
                                                // Directory optional — activity rows still show.
                                                LoadingOverlay.hide(overlay);
                                                showRows(activity);
                                            }
                                        });
                                return;
                            }
                            LoadingOverlay.hide(overlay);
                            showRows(activity);
                        }
                        @Override public void onError(String m) {
                            LoadingOverlay.hide(overlay);
                            showRows(new java.util.ArrayList<>());
                            LoadingOverlay.error(AdminCustomersActivity.this, m,
                                    () -> refresh());
                        }
                    });
            return;
        }
        showRows(AdminStats.customerSummaries(repo.getAllReservations()));
    }

    /** Every registered customer, enriched with observed reservation activity. */
    private static List<AdminStats.CustomerSummary> joinDirectory(
            java.util.Map<String, String> labels, List<AdminStats.CustomerSummary> activity) {
        java.util.Map<String, AdminStats.CustomerSummary> byUser = new java.util.HashMap<>();
        for (AdminStats.CustomerSummary c : activity) byUser.put(c.userId, c);
        List<AdminStats.CustomerSummary> rows = new ArrayList<>();
        for (java.util.Map.Entry<String, String> e : labels.entrySet()) {
            AdminStats.CustomerSummary c = byUser.get(e.getKey());
            if (c == null) c = new AdminStats.CustomerSummary(e.getKey());
            c.label = e.getValue();
            rows.add(c);
        }
        if (rows.isEmpty()) return activity; // no directory — fall back to observed only
        java.util.Collections.sort(rows, (a, b) -> b.reservations - a.reservations);
        return rows;
    }

    private void showRows(List<AdminStats.CustomerSummary> rows) {
        adapter.setRows(rows);
        boolean hasRows = !rows.isEmpty();
        list.setVisibility(hasRows ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasRows ? View.GONE : View.VISIBLE);
    }
}

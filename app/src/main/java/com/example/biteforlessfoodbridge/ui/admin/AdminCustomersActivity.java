package com.example.biteforlessfoodbridge.ui.admin;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.data.RepositoryProvider;
import com.example.biteforlessfoodbridge.data.remote.RemoteRepository;
import com.example.biteforlessfoodbridge.data.remote.SupaResult;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.example.biteforlessfoodbridge.util.AdminStats;
import com.google.android.material.appbar.MaterialToolbar;

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
            RepositoryProvider.remote(this).getAllReservations(
                    new SupaResult<java.util.List<Reservation>>() {
                        @Override public void onSuccess(java.util.List<Reservation> all) {
                            LoadingOverlay.hide(overlay);
                            showRows(AdminStats.customerSummaries(all));
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

    private void showRows(List<AdminStats.CustomerSummary> rows) {
        adapter.setRows(rows);
        boolean hasRows = !rows.isEmpty();
        list.setVisibility(hasRows ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasRows ? View.GONE : View.VISIBLE);
    }
}

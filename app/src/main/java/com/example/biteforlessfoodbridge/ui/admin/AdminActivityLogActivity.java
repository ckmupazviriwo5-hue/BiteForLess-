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
import com.example.biteforlessfoodbridge.model.ActivityEvent;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

/** Session activity log (admin actions performed, newest first). */
public class AdminActivityLogActivity extends AppCompatActivity {

    private DemoRepository repo;
    private boolean live;
    private ActivityEventAdapter adapter;
    private RecyclerView list;
    private View empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_log);

        repo = DemoRepository.getInstance();
        live = RepositoryProvider.isLive(this);

        MaterialToolbar toolbar = findViewById(R.id.admin_log_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        list = findViewById(R.id.admin_log_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActivityEventAdapter();
        list.setAdapter(adapter);
        empty = findViewById(R.id.admin_log_empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter == null) return;
        if (live) {
            View overlay = LoadingOverlay.show(this);
            RepositoryProvider.remote(this).getActivityLog(
                    new SupaResult<List<ActivityEvent>>() {
                        @Override public void onSuccess(List<ActivityEvent> events) {
                            LoadingOverlay.hide(overlay);
                            showRows(events);
                        }
                        @Override public void onError(String m) {
                            LoadingOverlay.hide(overlay);
                            showRows(new ArrayList<>());
                            LoadingOverlay.error(AdminActivityLogActivity.this, m,
                                    () -> onResume());
                        }
                    });
            return;
        }
        showRows(repo.getActivityLog());
    }

    private void showRows(List<ActivityEvent> events) {
        adapter.setRows(events);
        boolean hasRows = !events.isEmpty();
        list.setVisibility(hasRows ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasRows ? View.GONE : View.VISIBLE);
    }
}

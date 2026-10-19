package com.example.biteforlessfoodbridge;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.data.SessionManager;
import com.example.biteforlessfoodbridge.data.remote.AuthGateway;
import com.example.biteforlessfoodbridge.model.UserRole;
import com.example.biteforlessfoodbridge.ui.admin.AdminDashboardFragment;
import com.example.biteforlessfoodbridge.ui.customer.DealsFragment;
import com.example.biteforlessfoodbridge.ui.customer.HomeFragment;
import com.example.biteforlessfoodbridge.ui.customer.ProfileFragment;
import com.example.biteforlessfoodbridge.ui.customer.ReservationsFragment;
import com.example.biteforlessfoodbridge.ui.vendor.VendorDashboardFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Single-activity host. Customer roles get bottom-tab navigation;
 * vendor/admin roles get their demo dashboards (bottom nav hidden).
 */
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "extra_role";
    public static final String EXTRA_TAB = "extra_tab";
    public static final String TAB_HOME = "home";
    public static final String TAB_DEALS = "deals";
    public static final String TAB_RESERVATIONS = "reservations";

    private SessionManager session;
    private UserRole role;
    private BottomNavigationView bottomNav;
    private String pendingCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        session = new SessionManager(this);
        role = resolveRole(getIntent().getStringExtra(EXTRA_ROLE));
        session.setRole(role);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitleTextColor(com.google.android.material.color.MaterialColors.getColor(
                toolbar, com.google.android.material.R.attr.colorOnSurfaceVariant));
        boolean liveBackend = AuthGateway.isLive(this);
        toolbar.setSubtitle((liveBackend ? "Live: " : "Demo: ") + role.name());
        toolbar.setOnMenuItemClickListener(this::onToolbarItem);
        if (liveBackend) {
            // Demo data reset is meaningless against the real local database.
            toolbar.getMenu().findItem(R.id.action_reset_demo).setVisible(false);
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        this.bottomNav = bottomNav;

        if (role == UserRole.CUSTOMER) {
            bottomNav.setVisibility(View.VISIBLE);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) show(new HomeFragment());
                else if (id == R.id.nav_deals) show(new DealsFragment());
                else if (id == R.id.nav_reservations) show(new ReservationsFragment());
                else if (id == R.id.nav_profile) show(new ProfileFragment());
                else return false;
                return true;
            });
            if (savedInstanceState == null) {
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
            handleTabExtra(getIntent());
        } else {
            bottomNav.setVisibility(View.GONE);
            if (savedInstanceState == null) {
                show(role == UserRole.VENDOR ? new VendorDashboardFragment() : new AdminDashboardFragment());
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (role == UserRole.CUSTOMER) {
            handleTabExtra(intent);
        }
    }

    private void handleTabExtra(Intent intent) {
        if (intent == null) return;
        String tab = intent.getStringExtra(EXTRA_TAB);
        if (TAB_RESERVATIONS.equals(tab)) {
            openReservations();
        } else if (TAB_DEALS.equals(tab)) {
            openDealsWithCategory(intent.getStringExtra(DealsFragment.ARG_CATEGORY));
        } else if (TAB_HOME.equals(tab) && bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    /** Switches to Deals, optionally preselecting a category (Home shortcuts). */
    public void openDealsWithCategory(String categoryId) {
        pendingCategory = categoryId;
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_deals);
        }
    }

    /** Switches to the reservation history tab. */
    public void openReservations() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_reservations);
        }
    }

    public String consumePendingCategory() {
        String c = pendingCategory;
        pendingCategory = null;
        return c;
    }

    private void show(@NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host, fragment)
                .commit();
    }

    private boolean onToolbarItem(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sign_out) {
            session.signOut();
            if (AuthGateway.isLive(this)) {
                AuthGateway.auth(this).signOut();
            }
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        if (id == R.id.action_reset_demo) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Reset demo data?")
                    .setMessage("This clears all demo reservations and restores the seeded listings.")
                    .setPositiveButton("Reset", (d, w) -> {
                        DemoRepository.getInstance().resetDemoData();
                        Toast.makeText(this, "Demo data reset", Toast.LENGTH_SHORT).show();
                        recreate();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return false;
    }

    private UserRole resolveRole(String name) {
        if (name != null) {
            try {
                return UserRole.valueOf(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return session != null ? session.getRole() : UserRole.CUSTOMER;
    }
}

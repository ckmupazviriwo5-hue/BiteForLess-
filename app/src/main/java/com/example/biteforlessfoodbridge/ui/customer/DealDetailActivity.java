package com.example.biteforlessfoodbridge.ui.customer;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.biteforlessfoodbridge.MainActivity;
import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.data.RepositoryProvider;
import com.example.biteforlessfoodbridge.data.SessionManager;
import com.example.biteforlessfoodbridge.data.remote.RemoteRepository;
import com.example.biteforlessfoodbridge.data.remote.SupaResult;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.example.biteforlessfoodbridge.util.DateTimeUtil;
import com.example.biteforlessfoodbridge.util.FoodImage;
import com.example.biteforlessfoodbridge.util.PriceFormat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Listing detail with quantity selector and demo reservation.
 * Stock is always rechecked in the repository at reserve time.
 */
public class DealDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LISTING_ID = "extra_listing_id";

    private DemoRepository repo;
    private SessionManager session;
    private FoodListing listing;
    private int quantity = 1;
    private boolean live;
    private String liveVendorName = "";
    private String liveCategoryName = "";

    private TextView badge;
    private TextView name;
    private TextView description;
    private TextView vendor;
    private TextView location;
    private TextView window;
    private TextView deadline;
    private TextView status;
    private TextView price;
    private TextView originalPrice;
    private TextView savings;
    private TextView stock;
    private TextView qtyText;
    private TextView error;
    private View qtyRow;
    private Button reserve;
    private android.widget.ImageView detailImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal_detail);

        repo = DemoRepository.getInstance();
        session = new SessionManager(this);
        live = RepositoryProvider.isLive(this);

        MaterialToolbar toolbar = findViewById(R.id.detail_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        badge = findViewById(R.id.detail_badge);
        name = findViewById(R.id.detail_name);
        description = findViewById(R.id.detail_description);
        detailImage = findViewById(R.id.detail_image);
        vendor = findViewById(R.id.detail_vendor);
        location = findViewById(R.id.detail_location);
        window = findViewById(R.id.detail_window);
        deadline = findViewById(R.id.detail_deadline);
        status = findViewById(R.id.detail_status);
        price = findViewById(R.id.detail_price);
        originalPrice = findViewById(R.id.detail_original_price);
        savings = findViewById(R.id.detail_savings);
        stock = findViewById(R.id.detail_stock);
        qtyText = findViewById(R.id.detail_qty);
        error = findViewById(R.id.detail_error);
        qtyRow = findViewById(R.id.detail_qty_row);
        reserve = findViewById(R.id.detail_reserve);

        findViewById(R.id.detail_qty_minus).setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                qtyText.setText(String.valueOf(quantity));
            }
        });
        findViewById(R.id.detail_qty_plus).setOnClickListener(v -> {
            if (listing != null && quantity < listing.getAvailableQuantity()) {
                quantity++;
                qtyText.setText(String.valueOf(quantity));
            } else {
                Toast.makeText(this, "No more stock available", Toast.LENGTH_SHORT).show();
            }
        });
        reserve.setOnClickListener(v -> tryReserve());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        String id = getIntent().getStringExtra(EXTRA_LISTING_ID);
        quantity = 1;
        qtyText.setText("1");
        error.setVisibility(View.GONE);
        if (id == null) {
            finish();
            return;
        }
        if (!live) {
            listing = repo.getById(id);
            if (listing == null) {
                Toast.makeText(this, "Listing no longer exists", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            render();
            return;
        }
        View overlay = LoadingOverlay.show(this);
        RemoteRepository remote = RepositoryProvider.remote(this);
        remote.getListing(id, new SupaResult<FoodListing>() {
            @Override public void onSuccess(FoodListing value) {
                listing = value;
                remote.getVendor(value.getVendorId(), new SupaResult<Vendor>() {
                    @Override public void onSuccess(Vendor v) {
                        liveVendorName = v.getName();
                        remote.getCategoryName(value.getCategoryId(),
                                new SupaResult<String>() {
                                    @Override public void onSuccess(String name) {
                                        LoadingOverlay.hide(overlay);
                                        liveCategoryName = name;
                                        render();
                                    }
                                    @Override public void onError(String m) {
                                        LoadingOverlay.hide(overlay);
                                        render();
                                    }
                                });
                    }
                    @Override public void onError(String m) {
                        LoadingOverlay.hide(overlay);
                        render();
                    }
                });
            }
            @Override public void onError(String message) {
                LoadingOverlay.hide(overlay);
                Toast.makeText(DealDetailActivity.this, message, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void render() {
        long now = System.currentTimeMillis();
        boolean reservable = listing.isReservable(now);
        String vendorName;
        String categoryName;
        if (live) {
            vendorName = liveVendorName.isEmpty() ? "Unknown vendor" : liveVendorName;
            categoryName = liveCategoryName.isEmpty() ? listing.getCategoryId() : liveCategoryName;
        } else {
            Vendor v = repo.getVendorById(listing.getVendorId());
            vendorName = v == null ? "Unknown vendor" : v.getName();
            categoryName = repo.getCategoryName(listing.getCategoryId());
        }

        name.setText(listing.getName());
        description.setText(listing.getDescription());
        FoodImage.load(detailImage, listing, listing.getImageUrl());
        vendor.setText(vendorName + " · " + categoryName);
        location.setText("Pickup: " + listing.getLocation());
        window.setText("Window: " + listing.getCollectionWindow());
        deadline.setText(DateTimeUtil.formatDeadline(listing.getCollectionDeadlineEpoch())
                + " (" + DateTimeUtil.relativeToNow(listing.getCollectionDeadlineEpoch(), now) + ")");

        String statusText;
        if (listing.isExpired(now)) statusText = "Status: Expired";
        else if (listing.getStatus() != ListingStatus.ACTIVE) statusText = "Status: Paused by vendor";
        else if (listing.getAvailableQuantity() <= 0) statusText = "Status: Sold out";
        else statusText = "Status: Available";
        status.setText(statusText);

        String badgeText = PriceFormat.discountBadge(
                listing.getOriginalPrice(), listing.getDiscountedPrice());
        badge.setText(badgeText.isEmpty() ? categoryName : badgeText);

        price.setText(PriceFormat.format(listing.getDiscountedPrice()));
        originalPrice.setText(PriceFormat.format(listing.getOriginalPrice()));
        originalPrice.setPaintFlags(originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        savings.setText(PriceFormat.savings(listing.getOriginalPrice(), listing.getDiscountedPrice()));

        stock.setText(listing.getAvailableQuantity() <= 0
                ? "Out of stock"
                : listing.getAvailableQuantity() + " available");

        qtyRow.setVisibility(reservable ? View.VISIBLE : View.GONE);
        reserve.setEnabled(reservable);
        reserve.setText(reservable
                ? (live ? "Reserve pickup" : "Reserve pickup (demo)") : "Not available");
    }

    private void tryReserve() {
        error.setVisibility(View.GONE);
        if (listing == null) return;
        if (live) {
            View overlay = LoadingOverlay.show(this);
            RepositoryProvider.remote(this).rpcCreateReservation(
                    listing.getId(), quantity, new SupaResult<Reservation>() {
                        @Override public void onSuccess(Reservation r) {
                            LoadingOverlay.hide(overlay);
                            showConfirmation(r);
                        }
                        @Override public void onError(String message) {
                            LoadingOverlay.hide(overlay);
                            error.setText(message);
                            error.setVisibility(View.VISIBLE);
                            load(); // refresh displayed stock/status
                        }
                    });
            return;
        }
        // Final stock recheck happens inside the repository, not here.
        Reservation r;
        try {
            r = repo.create(listing.getId(), session.getEmail(), quantity);
        } catch (IllegalStateException e) {
            error.setText(e.getMessage());
            error.setVisibility(View.VISIBLE);
            load(); // refresh displayed stock/status
            return;
        }
        showConfirmation(r);
    }

    private void showConfirmation(Reservation r) {
        double total = listing.getDiscountedPrice() * r.getQuantity();
        String vendorLabel = live
                ? (liveVendorName.isEmpty() ? "Unknown vendor" : liveVendorName)
                : repo.getVendorName(listing.getVendorId());
        String message = "Reference: " + r.getPickupCode() + "\n"
                + listing.getName() + " × " + r.getQuantity() + "\n"
                + vendorLabel + "\n"
                + "Total: " + PriceFormat.format(total) + "\n"
                + "Pickup: " + listing.getLocation() + "\n"
                + (r.getPickupPin().isEmpty() ? "" : "Pickup PIN: " + r.getPickupPin() + "\n")
                + DateTimeUtil.formatDeadline(listing.getCollectionDeadlineEpoch()) + "\n"
                + "Status: " + r.getStatus().name() + "\n\n"
                + (live ? "Saved to your account — show this reference at pickup."
                : "Demo reservation only — show this reference at pickup.");
        new MaterialAlertDialogBuilder(this)
                .setTitle("Reservation confirmed")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("View reservations", (d, w) -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_RESERVATIONS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Close", (d, w) -> finish())
                .show();
    }
}

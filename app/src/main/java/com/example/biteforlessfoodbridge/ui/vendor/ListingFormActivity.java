package com.example.biteforlessfoodbridge.ui.vendor;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
import com.example.biteforlessfoodbridge.util.DateTimeUtil;
import com.example.biteforlessfoodbridge.util.PriceFormat;
import com.example.biteforlessfoodbridge.util.Validation;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reusable create/edit form for vendor listings. Cancelling never mutates
 * the repository; saving rechecks every rule in the repository at save time.
 */
public class ListingFormActivity extends AppCompatActivity {

    public static final String EXTRA_LISTING_ID = "extra_listing_id";

    private static final long HOUR = 3_600_000L;
    private static final String[] CREATE_DEADLINES =
            {"In 2 hours", "In 4 hours", "In 6 hours", "In 8 hours", "Tomorrow (+24h)"};
    private static final long[] CREATE_OFFSETS =
            {2 * HOUR, 4 * HOUR, 6 * HOUR, 8 * HOUR, 24 * HOUR};

    private DemoRepository repo;
    private String vendorId;
    private boolean live;
    private String liveVendorLocation = "";
    private FoodListing editing;

    private TextInputLayout nameLayout;
    private TextInputLayout originalLayout;
    private TextInputLayout discountedLayout;
    private TextInputLayout qtyLayout;
    private TextInputLayout locationLayout;
    private TextInputEditText name;
    private TextInputEditText description;
    private Spinner category;
    private TextInputEditText original;
    private TextInputEditText discounted;
    private TextView discountPreview;
    private TextInputEditText qty;
    private TextInputEditText location;
    private TextInputEditText window;
    private Spinner deadline;
    private TextView deadlineCurrent;
    private SwitchMaterial active;
    private TextView error;

    private List<Category> categories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_form);

        repo = DemoRepository.getInstance();
        live = RepositoryProvider.isLive(this);
        if (live) {
            initLive();
            return;
        }
        vendorId = new SessionManager(this).requireDemoVendorId();

        String listingId = getIntent().getStringExtra(EXTRA_LISTING_ID);
        if (listingId != null) {
            editing = repo.getById(listingId);
            if (editing == null || !vendorId.equals(editing.getVendorId())) {
                Toast.makeText(this, "Listing not available", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        bindViews();
        categories = repo.getCategories();
        setupCategorySpinner();
        setupForm(repo.getVendorById(vendorId) == null
                ? "" : repo.getVendorById(vendorId).getLocation());
    }

    /** Live init: owned vendor + categories (+ listing when editing). */
    private void initLive() {
        bindViews();
        View overlay = LoadingOverlay.show(this);
        RemoteRepository remote = RepositoryProvider.remote(this);
        remote.getMyVendor(new SupaResult<Vendor>() {
            @Override public void onSuccess(Vendor vendor) {
                vendorId = vendor.getId();
                remote.getCategories(new SupaResult<List<Category>>() {
                    @Override public void onSuccess(List<Category> cats) {
                        categories = cats;
                        String listingId = getIntent().getStringExtra(EXTRA_LISTING_ID);
                        if (listingId == null) {
                            LoadingOverlay.hide(overlay);
                            liveVendorLocation = vendor.getLocation();
                            setupCategorySpinner();
                            setupForm(vendor.getLocation());
                            return;
                        }
                        remote.getListing(listingId, new SupaResult<FoodListing>() {
                            @Override public void onSuccess(FoodListing value) {
                                LoadingOverlay.hide(overlay);
                                if (!vendorId.equals(value.getVendorId())) {
                                    Toast.makeText(ListingFormActivity.this,
                                            "Listing not available", Toast.LENGTH_SHORT).show();
                                    finish();
                                    return;
                                }
                                editing = value;
                                liveVendorLocation = vendor.getLocation();
                                setupCategorySpinner();
                                setupForm(vendor.getLocation());
                            }
                            @Override public void onError(String m) {
                                LoadingOverlay.hide(overlay);
                                Toast.makeText(ListingFormActivity.this, m,
                                        Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                    @Override public void onError(String m) {
                        LoadingOverlay.hide(overlay);
                        Toast.makeText(ListingFormActivity.this, m,
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }
            @Override public void onError(String m) {
                LoadingOverlay.hide(overlay);
                Toast.makeText(ListingFormActivity.this, m, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void setupCategorySpinner() {
        List<String> names = new ArrayList<>();
        for (Category c : categories) names.add(c.getName());
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        category.setAdapter(catAdapter);
    }

    private void setupForm(String vendorLocation) {
        if (editing == null) {
            setSpinnerOptions(CREATE_DEADLINES);
            deadline.setSelection(2); // +6h default
            active.setChecked(true);
            location.setText(vendorLocation == null ? "" : vendorLocation);
        } else {
            ((TextView) findViewById(R.id.form_title)).setText("Edit listing");
            List<String> editOptions = new ArrayList<>();
            editOptions.add("Keep current deadline");
            for (String s : CREATE_DEADLINES) editOptions.add(s);
            setSpinnerOptions(editOptions.toArray(new String[0]));
            deadline.setSelection(0);
            deadlineCurrent.setVisibility(View.VISIBLE);
            deadlineCurrent.setText("Current: " + DateTimeUtil.formatDeadline(
                    editing.getCollectionDeadlineEpoch()));
            prefill();
        }

        TextWatcher previewWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { updatePreview(); }
        };
        original.addTextChangedListener(previewWatcher);
        discounted.addTextChangedListener(previewWatcher);
        updatePreview();

        findViewById(R.id.form_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.form_save).setOnClickListener(v -> save());
    }

    private void bindViews() {
        nameLayout = findViewById(R.id.form_name_layout);
        originalLayout = findViewById(R.id.form_original_layout);
        discountedLayout = findViewById(R.id.form_discounted_layout);
        qtyLayout = findViewById(R.id.form_qty_layout);
        locationLayout = findViewById(R.id.form_location_layout);
        name = findViewById(R.id.form_name);
        description = findViewById(R.id.form_description);
        category = findViewById(R.id.form_category);
        original = findViewById(R.id.form_original);
        discounted = findViewById(R.id.form_discounted);
        discountPreview = findViewById(R.id.form_discount_preview);
        qty = findViewById(R.id.form_qty);
        location = findViewById(R.id.form_location);
        window = findViewById(R.id.form_window);
        deadline = findViewById(R.id.form_deadline);
        deadlineCurrent = findViewById(R.id.form_deadline_current);
        active = findViewById(R.id.form_active);
        error = findViewById(R.id.form_error);
    }

    private void setSpinnerOptions(String[] options) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deadline.setAdapter(a);
    }

    private void prefill() {
        name.setText(editing.getName());
        description.setText(editing.getDescription());
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId().equals(editing.getCategoryId())) {
                category.setSelection(i);
                break;
            }
        }
        original.setText(trim(editing.getOriginalPrice()));
        discounted.setText(trim(editing.getDiscountedPrice()));
        qty.setText(String.valueOf(editing.getAvailableQuantity()));
        location.setText(editing.getLocation());
        window.setText(editing.getCollectionWindow());
        active.setChecked(editing.getStatus() == ListingStatus.ACTIVE);
    }

    private String trim(double value) {
        if (value == Math.floor(value)) return String.valueOf((long) value);
        return String.valueOf(value);
    }

    private void updatePreview() {
        Double o = parseDouble(original);
        Double d = parseDouble(discounted);
        if (o != null && d != null && Validation.isPricePairValid(o, d)) {
            discountPreview.setText(PriceFormat.discountBadge(o, d) + " · "
                    + PriceFormat.savings(o, d));
        } else {
            discountPreview.setText("");
        }
    }

    private Double parseDouble(TextInputEditText et) {
        String raw = et.getText() == null ? "" : et.getText().toString().trim();
        if (raw.isEmpty()) return null;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(TextInputEditText et) {
        String raw = et.getText() == null ? "" : et.getText().toString().trim();
        if (raw.isEmpty()) return null;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void save() {
        error.setVisibility(View.GONE);
        boolean ok = true;

        String nameVal = textOf(name);
        if (!Validation.isRequired(nameVal)) {
            nameLayout.setError("Food name is required");
            ok = false;
        } else {
            nameLayout.setError(null);
        }

        Double origVal = parseDouble(original);
        if (origVal == null || origVal <= 0) {
            originalLayout.setError("Enter a price above K0");
            ok = false;
        } else {
            originalLayout.setError(null);
        }

        Double discVal = parseDouble(discounted);
        if (discVal == null || discVal < 0) {
            discountedLayout.setError("Enter a valid price");
            ok = false;
        } else {
            discountedLayout.setError(null);
        }

        if (origVal != null && discVal != null && origVal > 0
                && !Validation.isPricePairValid(origVal, discVal)) {
            discountedLayout.setError("Must be below the original price");
            ok = false;
        }

        Integer qtyVal = parseInt(qty);
        if (qtyVal == null || qtyVal < 0) {
            qtyLayout.setError("Enter 0 or more");
            ok = false;
        } else {
            qtyLayout.setError(null);
        }

        String locVal = textOf(location);
        if (!Validation.isRequired(locVal)) {
            locationLayout.setError("Collection location is required");
            ok = false;
        } else {
            locationLayout.setError(null);
        }

        if (!ok) return;

        int catPos = Math.max(0, category.getSelectedItemPosition());
        String categoryId = categories.get(Math.min(catPos, categories.size() - 1)).getId();
        long deadlineEpoch = resolveDeadline();
        if (deadlineEpoch <= 0) {
            showError("Choose a collection deadline");
            return;
        }
        ListingStatus status = active.isChecked() ? ListingStatus.ACTIVE : ListingStatus.INACTIVE;
        String windowVal = textOf(window);
        if (windowVal.isEmpty()) windowVal = "See vendor for times";
        String descVal = textOf(description);

        if (status == ListingStatus.ACTIVE && qtyVal < 1 && editing == null) {
            qtyLayout.setError("Active listings need at least 1 in stock");
            return;
        }

        if (live) {
            saveLive(nameVal, descVal, categoryId, origVal, discVal, qtyVal,
                    locVal, deadlineEpoch, windowVal, status);
            return;
        }

        try {
            if (editing == null) {
                repo.createListing(vendorId, nameVal, descVal, categoryId, origVal,
                        discVal, qtyVal, locVal, deadlineEpoch, windowVal, status);
                Toast.makeText(this, "Listing created (demo)", Toast.LENGTH_SHORT).show();
            } else {
                repo.updateListing(editing.getId(), vendorId, nameVal, descVal, categoryId,
                        origVal, discVal, qtyVal, locVal, deadlineEpoch, windowVal);
                repo.setListingStatus(editing.getId(), status, vendorId);
                Toast.makeText(this, "Listing updated (demo)", Toast.LENGTH_SHORT).show();
            }
        } catch (IllegalStateException e) {
            showError(e.getMessage());
            return;
        }
        finish();
    }

    /** Live save: direct table write (owner RLS) + status via secure RPC. */
    private void saveLive(String nameVal, String descVal, String categoryId, double origVal,
                          double discVal, int qtyVal, String locVal, long deadlineEpoch,
                          String windowVal, ListingStatus status) {
        View overlay = LoadingOverlay.show(this);
        RemoteRepository remote = RepositoryProvider.remote(this);
        try {
            org.json.JSONObject fields = new org.json.JSONObject()
                    .put("vendor_id", vendorId)
                    .put("name", nameVal)
                    .put("description", descVal)
                    .put("category_id", categoryId)
                    .put("original_price", origVal)
                    .put("discounted_price", discVal)
                    .put("stock", qtyVal)
                    .put("location", locVal)
                    .put("deadline", RemoteRepository.isoFor(deadlineEpoch))
                    .put("collection_window", windowVal)
                    .put("currency", "ZMW")
                    .put("status", status == ListingStatus.ACTIVE ? "active" : "inactive");
            if (editing == null) {
                fields.put("id", "vl-" + System.currentTimeMillis());
                remote.createListing(fields, new SupaResult<FoodListing>() {
                    @Override public void onSuccess(FoodListing v) {
                        LoadingOverlay.hide(overlay);
                        Toast.makeText(ListingFormActivity.this, "Listing created",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override public void onError(String m) {
                        LoadingOverlay.hide(overlay);
                        showError(m);
                    }
                });
            } else {
                remote.updateListing(editing.getId(), fields,
                        new SupaResult<FoodListing>() {
                            @Override public void onSuccess(FoodListing v) {
                                remote.rpcSetListingStatus(editing.getId(), status,
                                        new SupaResult<String>() {
                                            @Override public void onSuccess(String s) {
                                                LoadingOverlay.hide(overlay);
                                                Toast.makeText(ListingFormActivity.this,
                                                        "Listing updated",
                                                        Toast.LENGTH_SHORT).show();
                                                finish();
                                            }
                                            @Override public void onError(String m) {
                                                LoadingOverlay.hide(overlay);
                                                showError(m);
                                            }
                                        });
                            }
                            @Override public void onError(String m) {
                                LoadingOverlay.hide(overlay);
                                showError(m);
                            }
                        });
            }
        } catch (org.json.JSONException e) {
            LoadingOverlay.hide(overlay);
            showError("Could not save listing");
        }
    }

    private long resolveDeadline() {
        long now = System.currentTimeMillis();
        int pos = deadline.getSelectedItemPosition();
        if (editing != null) {
            if (pos <= 0) return editing.getCollectionDeadlineEpoch(); // keep current
            pos -= 1;
        }
        if (pos < 0 || pos >= CREATE_OFFSETS.length) return -1;
        return now + CREATE_OFFSETS[pos];
    }

    private void showError(String message) {
        error.setText(message == null ? "Could not save listing" : message);
        error.setVisibility(View.VISIBLE);
    }
}

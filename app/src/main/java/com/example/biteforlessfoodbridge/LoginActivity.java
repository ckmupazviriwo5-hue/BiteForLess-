package com.example.biteforlessfoodbridge;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.example.biteforlessfoodbridge.data.SessionManager;
import com.example.biteforlessfoodbridge.data.remote.SupaResult;
import com.example.biteforlessfoodbridge.data.remote.SupabaseAuth;
import com.example.biteforlessfoodbridge.data.remote.SupabaseConfig;
import com.example.biteforlessfoodbridge.model.UserRole;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.example.biteforlessfoodbridge.util.Validation;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private SupabaseAuth supaAuth;
    private boolean live;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        live = SupabaseConfig.isConfigured();
        supaAuth = new SupabaseAuth(this);

        TextInputLayout emailLayout = findViewById(R.id.email_layout);
        TextInputLayout passwordLayout = findViewById(R.id.password_layout);
        TextInputEditText etEmail = findViewById(R.id.et_email);
        TextInputEditText etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView btnGotoSignup = findViewById(R.id.btn_goto_signup);
        RadioGroup roleGroup = findViewById(R.id.role_group);
        Button btnGoogle = findViewById(R.id.btn_google);

        // Live backend: real auth; role comes from the server profile.
        // Demo mode: simulated role picker as before.
        if (live) {
            roleGroup.setVisibility(View.GONE);
            findViewById(R.id.role_label).setVisibility(View.GONE);
            btnGoogle.setVisibility(View.VISIBLE);
        }

        // Scale animation on touch
        View.OnTouchListener touchAnimation = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
            }
            return false;
        };

        btnLogin.setOnTouchListener(touchAnimation);
        btnGotoSignup.setOnTouchListener(touchAnimation);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
            String password = etPassword.getText() == null ? "" : etPassword.getText().toString();

            String emailError = Validation.emailError(email);
            if (emailError != null) {
                emailLayout.setError(emailError);
                return;
            }
            emailLayout.setError(null);

            if (!Validation.isPasswordLengthOk(password, 6)) {
                passwordLayout.setError("Password must be at least 6 characters");
                return;
            }
            passwordLayout.setError(null);

            if (live) {
                View overlay = LoadingOverlay.show(this);
                supaAuth.signIn(email, password, new SupaResult<String>() {
                    @Override public void onSuccess(String signedEmail) {
                        LoadingOverlay.hide(overlay);
                        afterLiveAuth(signedEmail);
                    }
                    @Override public void onError(String message) {
                        LoadingOverlay.hide(overlay);
                        Toast.makeText(LoginActivity.this, message,
                                Toast.LENGTH_LONG).show();
                    }
                });
                return;
            }

            UserRole role = selectedRole(roleGroup);
            // Simulated sign-in (demo only — no real authentication).
            new SessionManager(this).signIn("", email, role);

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_ROLE, role.name());
            startActivity(intent);
            finish();
        });

        btnGoogle.setOnClickListener(v -> {
            supaAuth.beginGoogle(new SupaResult<String>() {
                @Override public void onSuccess(String url) {
                    try {
                        new CustomTabsIntent.Builder().build()
                                .launchUrl(LoginActivity.this, Uri.parse(url));
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this,
                                "No browser available for Google sign-in",
                                Toast.LENGTH_LONG).show();
                    }
                }
                @Override public void onError(String message) {
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });

        btnGotoSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }

    /** Resolves the server-authoritative role, then enters the app. */
    private void afterLiveAuth(String email) {
        View overlay = LoadingOverlay.show(this);
        supaAuth.fetchRole(new SupaResult<String>() {
            @Override public void onSuccess(String role) {
                LoadingOverlay.hide(overlay);
                UserRole mapped = "admin".equalsIgnoreCase(role) ? UserRole.ADMIN
                        : "vendor".equalsIgnoreCase(role) ? UserRole.VENDOR
                        : UserRole.CUSTOMER;
                new SessionManager(LoginActivity.this)
                        .signIn("", email == null ? "" : email, mapped);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_ROLE, mapped.name());
                startActivity(intent);
                finish();
            }
            @Override public void onError(String message) {
                LoadingOverlay.hide(overlay);
                Toast.makeText(LoginActivity.this,
                        "Signed in, but role lookup failed: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private UserRole selectedRole(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == R.id.role_vendor) return UserRole.VENDOR;
        if (id == R.id.role_admin) return UserRole.ADMIN;
        return UserRole.CUSTOMER;
    }
}

package com.example.biteforlessfoodbridge;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.biteforlessfoodbridge.data.SessionManager;
import com.example.biteforlessfoodbridge.data.remote.AuthGateway;
import com.example.biteforlessfoodbridge.data.remote.SupaResult;
import com.example.biteforlessfoodbridge.data.remote.SupabaseAuth;
import com.example.biteforlessfoodbridge.model.UserRole;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;
import com.example.biteforlessfoodbridge.util.Validation;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SignupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        TextInputLayout nameLayout = findViewById(R.id.name_layout);
        TextInputLayout emailLayout = findViewById(R.id.email_layout);
        TextInputLayout phoneLayout = findViewById(R.id.phone_layout);
        TextInputLayout passwordLayout = findViewById(R.id.password_layout);
        TextInputLayout confirmLayout = findViewById(R.id.confirm_password_layout);
        TextInputEditText etName = findViewById(R.id.et_name);
        TextInputEditText etEmail = findViewById(R.id.et_email);
        TextInputEditText etPhone = findViewById(R.id.et_phone);
        TextInputEditText etPassword = findViewById(R.id.et_password);
        TextInputEditText etConfirmPassword = findViewById(R.id.et_confirm_password);
        Button btnSignup = findViewById(R.id.btn_signup);
        TextView btnGotoLogin = findViewById(R.id.btn_goto_login);

        // Scale animation on touch
        View.OnTouchListener touchAnimation = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
            }
            return false;
        };

        btnSignup.setOnTouchListener(touchAnimation);
        btnGotoLogin.setOnTouchListener(touchAnimation);

        btnSignup.setOnClickListener(v -> {
            String name = textOf(etName).trim();
            String email = textOf(etEmail).trim();
            String phone = textOf(etPhone).trim();
            String password = textOf(etPassword);
            String confirmPassword = textOf(etConfirmPassword);

            boolean ok = true;

            if (!Validation.isRequired(name)) {
                nameLayout.setError("Full name is required");
                ok = false;
            } else {
                nameLayout.setError(null);
            }

            String emailError = Validation.emailError(email);
            emailLayout.setError(emailError);
            if (emailError != null) ok = false;

            // The local API requires a phone number for pickup contact.
            if (!Validation.isRequired(phone)) {
                phoneLayout.setError("Phone number is required");
                ok = false;
            } else {
                phoneLayout.setError(null);
            }

            if (!Validation.isPasswordLengthOk(password, 6)) {
                passwordLayout.setError("Password must be at least 6 characters");
                ok = false;
            } else {
                passwordLayout.setError(null);
            }

            if (!password.equals(confirmPassword)) {
                confirmLayout.setError("Passwords do not match");
                ok = false;
            } else {
                confirmLayout.setError(null);
            }

            if (!ok) return;

            if (AuthGateway.isLive(this)) {
                View overlay = LoadingOverlay.show(this);
                String finalName = name;
                String finalEmail = email;
                SupabaseAuth auth = AuthGateway.auth(this);
                auth.signUp(name, email, phone, password,
                        new SupaResult<String>() {
                            @Override public void onSuccess(String signedEmail) {
                                LoadingOverlay.hide(overlay);
                                // New server profiles are customers by trigger.
                                new SessionManager(SignupActivity.this).signIn(
                                        finalName, finalEmail, UserRole.CUSTOMER);
                                Toast.makeText(SignupActivity.this,
                                        "Account created — welcome!",
                                        Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(SignupActivity.this,
                                        MainActivity.class).putExtra(MainActivity.EXTRA_ROLE,
                                        UserRole.CUSTOMER.name()));
                                finish();
                            }
                            @Override public void onError(String message) {
                                LoadingOverlay.hide(overlay);
                                Toast.makeText(SignupActivity.this, message,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                return;
            }

            // Simulated registration (demo only — nothing leaves the device).
            new SessionManager(this).signIn(name, email, UserRole.CUSTOMER);

            Toast.makeText(this, "Registration successful (demo)", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SignupActivity.this, MainActivity.class)
                    .putExtra(MainActivity.EXTRA_ROLE, UserRole.CUSTOMER.name()));
            finish();
        });

        btnGotoLogin.setOnClickListener(v -> finish());
    }

    private String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
}

package com.example.biteforlessfoodbridge;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.biteforlessfoodbridge.data.SessionManager;
import com.example.biteforlessfoodbridge.data.remote.SupaResult;
import com.example.biteforlessfoodbridge.data.remote.SupabaseAuth;
import com.example.biteforlessfoodbridge.model.UserRole;
import com.example.biteforlessfoodbridge.ui.common.LoadingOverlay;

/**
 * Receives the OAuth deep-link callback (biteforless://auth-callback),
 * exchanges the PKCE code for a session, then enters the app with the
 * server-authoritative role.
 */
public class OAuthCallbackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri data = getIntent() == null ? null : getIntent().getData();
        String error = SupabaseAuth.callbackError(data);
        if (!error.isEmpty()) {
            Toast.makeText(this, "Google sign-in cancelled: " + error,
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String code = SupabaseAuth.callbackCode(data);
        if (code.isEmpty()) {
            Toast.makeText(this, "Google sign-in was not completed",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        android.view.View overlay = LoadingOverlay.show(this);
        SupabaseAuth auth = new SupabaseAuth(this);
        auth.exchangeCode(code, new SupaResult<String>() {
            @Override public void onSuccess(String email) {
                auth.fetchRole(new SupaResult<String>() {
                    @Override public void onSuccess(String role) {
                        LoadingOverlay.hide(overlay);
                        UserRole mapped = "admin".equalsIgnoreCase(role) ? UserRole.ADMIN
                                : "vendor".equalsIgnoreCase(role) ? UserRole.VENDOR
                                : UserRole.CUSTOMER;
                        new SessionManager(OAuthCallbackActivity.this)
                                .signIn("", email == null ? "" : email, mapped);
                        Intent intent = new Intent(OAuthCallbackActivity.this,
                                MainActivity.class);
                        intent.putExtra(MainActivity.EXTRA_ROLE, mapped.name());
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                    @Override public void onError(String message) {
                        LoadingOverlay.hide(overlay);
                        Toast.makeText(OAuthCallbackActivity.this,
                                "Signed in, but role lookup failed: " + message,
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }
            @Override public void onError(String message) {
                LoadingOverlay.hide(overlay);
                Toast.makeText(OAuthCallbackActivity.this, message,
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}

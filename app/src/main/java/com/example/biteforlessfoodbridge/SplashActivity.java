package com.example.biteforlessfoodbridge;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.biteforlessfoodbridge.data.SessionManager;
import com.example.biteforlessfoodbridge.data.remote.SupaResult;
import com.example.biteforlessfoodbridge.data.remote.SupabaseAuth;
import com.example.biteforlessfoodbridge.data.remote.SupabaseConfig;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logo);

        // Simple Scale Animation
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0.5f, 1.0f, 0.5f, 1.0f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(800);
        logo.startAnimation(scaleAnimation);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (SupabaseConfig.isConfigured()) {
                // Live backend: validate/refresh the stored session.
                new SupabaseAuth(this).restore(new SupaResult<Boolean>() {
                    @Override public void onSuccess(Boolean live) {
                        goTo(live ? MainActivity.class : LoginActivity.class);
                    }
                    @Override public void onError(String message) {
                        goTo(LoginActivity.class);
                    }
                });
                return;
            }
            SessionManager session = new SessionManager(this);
            goTo(session.isSignedIn() ? MainActivity.class : LoginActivity.class);
        }, 1200);
    }

    private void goTo(Class<?> next) {
        startActivity(new Intent(SplashActivity.this, next));
        finish();
    }
}

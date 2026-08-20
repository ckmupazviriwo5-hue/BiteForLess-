package com.example.biteforlessfoodbridge;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button btnLogin = findViewById(R.id.btn_login);
        TextView btnGotoSignup = findViewById(R.id.btn_goto_signup);

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
            // Placeholder for login logic
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });

        btnGotoSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }
}
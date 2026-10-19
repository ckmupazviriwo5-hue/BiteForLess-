package com.example.biteforlessfoodbridge.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.biteforlessfoodbridge.LoginActivity;
import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.data.SessionManager;
import com.example.biteforlessfoodbridge.data.remote.AuthGateway;
import com.example.biteforlessfoodbridge.util.Validation;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Local demo profile editor (SharedPreferences-backed). */
public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        SessionManager session = new SessionManager(requireContext());
        TextInputLayout emailLayout = root.findViewById(R.id.profile_email_layout);
        TextInputEditText name = root.findViewById(R.id.profile_name);
        TextInputEditText email = root.findViewById(R.id.profile_email);
        TextView role = root.findViewById(R.id.profile_role);
        Button save = root.findViewById(R.id.profile_save);
        Button signOut = root.findViewById(R.id.profile_sign_out);

        name.setText(session.getName());
        email.setText(session.getEmail());
        boolean liveBackend = AuthGateway.isLive(requireContext());
        role.setText("Role: " + session.getRole().name() + (liveBackend ? "" : " (demo)"));
        if (liveBackend) {
            // Profile fields are server-owned on a live backend — read-only here.
            name.setEnabled(false);
            email.setEnabled(false);
            save.setEnabled(false);
        }

        save.setOnClickListener(v -> {
            String newName = name.getText() == null ? "" : name.getText().toString().trim();
            String newEmail = email.getText() == null ? "" : email.getText().toString().trim();
            if (!Validation.isRequired(newName)) {
                Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            String emailError = Validation.emailError(newEmail);
            if (emailError != null) {
                emailLayout.setError(emailError);
                return;
            }
            emailLayout.setError(null);
            session.setProfile(newName, newEmail);
            Toast.makeText(requireContext(), "Profile saved (demo)", Toast.LENGTH_SHORT).show();
        });

        signOut.setOnClickListener(v -> {
            session.signOut();
            if (AuthGateway.isLive(requireContext())) {
                AuthGateway.auth(requireContext()).signOut();
            }
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });

        return root;
    }
}

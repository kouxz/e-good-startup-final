package com.projeto.egoodapp.views.profile;

import com.projeto.egoodapp.views.common.navigation.UserChrome;

import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.local.LocalSession;
import com.projeto.egoodapp.views.account.LocalProfileForms;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.theme.AppThemeController;
import com.projeto.egoodapp.views.account.AccountDeletionUi;
import com.projeto.egoodapp.views.auth.LoginActivity;
import com.projeto.egoodapp.views.home.HomeActivity;
import com.projeto.egoodapp.views.vehicle.VehiclesActivity;
import com.projeto.egoodapp.views.comparison.ComparisonActivity;
import com.projeto.egoodapp.views.solar.SolarActivity;

public class ProfileActivity extends com.projeto.egoodapp.views.common.session.AuthenticatedActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private TextView tvUserName;
    private TextView tvUserInitials;
    private Button btnLogout;
    private SwitchCompat darkModeSwitch;
    private UserChrome chrome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        setupViews();
        if (!loadUserData()) return;
        setupNavigation();
        chrome = new UserChrome(this, UserChrome.Section.PROFILE);
        setupPreferences();
        new UserSupport(this);
        setupLogout();
        new AccountDeletionUi(this, findViewById(R.id.btnDeleteAccount), false);
    }

    private void setupViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvUserInitials = findViewById(R.id.tvUserInitials);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private boolean loadUserData() {
        if (currentUser == null) { logout(); return false; }
        AccountProfile profile = LocalSession.current(this, "pessoa");
        if (profile == null) { logout(); return false; }
        String name = profile.name.isEmpty() ? "Usuário" : profile.name;
        tvUserName.setText(name); tvUserInitials.setText(getInitials(name));
        ((TextView) findViewById(R.id.tvLocalUserPhone)).setText(profile.phone.isEmpty() ? "Não informado" : profile.phone);
        ((TextView) findViewById(R.id.tvLocalUserEmail)).setText(profile.email);
        return true;
    }
    @Override protected void onResume() {
        super.onResume();
        loadUserData();
        if (chrome != null) chrome.refresh();
        if (darkModeSwitch != null) {
            darkModeSwitch.setChecked(AppThemeController.isDarkModeEnabled(this));
        }
    }

    private void setupNavigation() {
        LinearLayout btnHome = findViewById(R.id.btnHome);
        LinearLayout btnVehicles = findViewById(R.id.btnVehicles);
        LinearLayout btnComparison = findViewById(R.id.btnComparison);
        LinearLayout btnSolar = findViewById(R.id.btnSolar);
        LinearLayout btnProfile = findViewById(R.id.btnProfile);

        btnHome.setOnClickListener(v -> navigateToHome());
        btnVehicles.setOnClickListener(v -> navigateToVehicles());
        btnComparison.setOnClickListener(v -> navigateToComparison());
        btnSolar.setOnClickListener(v -> navigateToSolar());
        btnProfile.setOnClickListener(v -> {});
    }

    private void setupPreferences() {
        SwitchCompat swNotifications = findViewById(R.id.swNotifications);
        darkModeSwitch = findViewById(R.id.swDarkMode);

        swNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Handle notifications preference
        });

        darkModeSwitch.setChecked(AppThemeController.isDarkModeEnabled(this));
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked != AppThemeController.isDarkModeEnabled(this)) {
                AppThemeController.setDarkModeEnabled(this, isChecked);
            }
        });

        // Setup edit profile click listeners
        LinearLayout userProfileSection = findViewById(R.id.userProfileSection);
        if (userProfileSection != null) {
            userProfileSection.setOnClickListener(v -> showEditProfileDialog());
        }

        LinearLayout accountDataSection = findViewById(R.id.accountDataSection);
        if (accountDataSection != null) {
            accountDataSection.setOnClickListener(v -> showEditProfileDialog());
        }
    }

    private void setupLogout() {
        btnLogout.setOnClickListener(v -> logout());
    }

    private void showEditProfileDialog() {
        AccountProfile profile = currentUser == null ? null : LocalSession.current(this, "pessoa");
        if (profile == null) { logout(); return; }
        LocalProfileForms.edit(this, profile, this::loadUserData);
    }

    private void logout() {
        com.projeto.egoodapp.data.local.LocalSession.logout(this);
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void navigateToVehicles() {
        startActivity(new Intent(this, VehiclesActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void navigateToComparison() {
        startActivity(new Intent(this, ComparisonActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void navigateToSolar() {
        startActivity(new Intent(this, SolarActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private String getInitials(String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        String first = parts[0].substring(0, 1);
        String second = parts[1].substring(0, 1);
        return (first + second).toUpperCase();
    }
}







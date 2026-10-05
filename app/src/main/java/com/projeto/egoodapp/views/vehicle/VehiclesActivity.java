package com.projeto.egoodapp.views.vehicle;

import com.projeto.egoodapp.views.solar.SolarActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.VehicleCatalog;
import com.projeto.egoodapp.data.model.Vehicle;
import com.projeto.egoodapp.views.comparison.ComparisonActivity;
import com.projeto.egoodapp.views.home.HomeActivity;
import com.projeto.egoodapp.views.profile.ProfileActivity;
import com.projeto.egoodapp.views.common.navigation.UserChrome;
import java.util.ArrayList;
import java.util.List;

public class VehiclesActivity extends com.projeto.egoodapp.views.common.session.AuthenticatedActivity {
    private TextInputEditText etSearch;
    private MaterialButton btnFilterAll;
    private MaterialButton btnFilterHatch;
    private MaterialButton btnFilterSUV;
    private MaterialButton btnFilterCompact;
    private MaterialButton btnFilterLuxury;
    private View emptyState;
    private VehicleCatalogAdapter adapter;
    private String currentFilter = "Todos";
    private UserChrome chrome;
    private List<Vehicle> vehicles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicles);
        if (savedInstanceState != null) {
            currentFilter = savedInstanceState.getString("category", "Todos");
        }

        initializeViews();
        setupNavigation();
        chrome = new UserChrome(this, UserChrome.Section.VEHICLES);
        setupFilters();
        updateFilterButtons();
    }

    private void initializeViews() {
        etSearch = findViewById(R.id.etSearch);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterHatch = findViewById(R.id.btnFilterHatch);
        btnFilterSUV = findViewById(R.id.btnFilterSUV);
        btnFilterCompact = findViewById(R.id.btnFilterCompact);
        btnFilterLuxury = findViewById(R.id.btnFilterLuxury);
        emptyState = findViewById(R.id.vehicleEmptyState);

        btnFilterAll.setCheckable(true);
        btnFilterHatch.setCheckable(true);
        btnFilterSUV.setCheckable(true);
        btnFilterCompact.setCheckable(true);
        btnFilterLuxury.setCheckable(true);

        RecyclerView recycler = findViewById(R.id.vehicleRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setHasFixedSize(false);
        adapter = new VehicleCatalogAdapter(this::openVehicle);
        recycler.setAdapter(adapter);

        TextView tvUserAvatar = findViewById(R.id.tvUserAvatar);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getDisplayName() != null
                && !currentUser.getDisplayName().isEmpty()) {
            tvUserAvatar.setText(getInitials(currentUser.getDisplayName()));
        }
    }

    private void setupFilters() {
        btnFilterAll.setOnClickListener(v -> applyFilter("Todos"));
        btnFilterHatch.setOnClickListener(v -> applyFilter("Hatch"));
        btnFilterSUV.setOnClickListener(v -> applyFilter("SUV"));
        btnFilterCompact.setOnClickListener(v -> applyFilter("SUV compacto"));
        btnFilterLuxury.setOnClickListener(v -> applyFilter("Luxo"));

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                displayVehicles();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        updateFilterButtons();
        displayVehicles();
    }

    private void updateFilterButtons() {
        btnFilterAll.setChecked("Todos".equals(currentFilter));
        btnFilterHatch.setChecked("Hatch".equals(currentFilter));
        btnFilterSUV.setChecked("SUV".equals(currentFilter));
        btnFilterCompact.setChecked("SUV compacto".equals(currentFilter));
        btnFilterLuxury.setChecked("Luxo".equals(currentFilter));
    }

    private void displayVehicles() {
        String dealerId = getIntent().getStringExtra("dealerId");
        String query = etSearch.getText() == null ? "" : etSearch.getText().toString();
        List<Vehicle> filtered = VehicleCatalog.filter(vehicles, currentFilter, query, dealerId);
        adapter.submitList(filtered);
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openVehicle(Vehicle vehicle) {
        startActivity(new Intent(this, VehicleDetailActivity.class)
                .putExtra("vehicleId", vehicle.getId()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (chrome != null) chrome.refresh();
        vehicles = LocalRepository.get(this).catalog();
        displayVehicles();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString("category", currentFilter);
        super.onSaveInstanceState(state);
    }

    private void setupNavigation() {
        LinearLayout btnHome = findViewById(R.id.btnHome);
        LinearLayout btnVehicles = findViewById(R.id.btnVehicles);
        LinearLayout btnComparison = findViewById(R.id.btnComparison);
        LinearLayout btnSolar = findViewById(R.id.btnSolar);
        LinearLayout btnProfile = findViewById(R.id.btnProfile);

        btnHome.setOnClickListener(v -> navigateTo(HomeActivity.class));
        btnVehicles.setOnClickListener(v -> {});
        btnComparison.setOnClickListener(v -> navigateTo(ComparisonActivity.class));
        btnSolar.setOnClickListener(v -> navigateTo(SolarActivity.class));
        btnProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
    }

    private void navigateTo(Class<?> destination) {
        startActivity(new Intent(this, destination)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private String getInitials(String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }
}

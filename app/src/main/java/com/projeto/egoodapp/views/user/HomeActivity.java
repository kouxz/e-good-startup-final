package com.projeto.egoodapp.views.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.local.AccountProfile;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.LocalSession;
import com.projeto.egoodapp.models.Vehicle;
import com.projeto.egoodapp.views.VehiclePhotos;
import com.projeto.egoodapp.views.comparison.ComparisonActivity;
import com.projeto.egoodapp.views.dealership.ConcessionariaDetailActivity;
import com.projeto.egoodapp.views.dealership.ConcessionariasActivity;
import com.projeto.egoodapp.views.vehicle.SolarActivity;
import com.projeto.egoodapp.views.vehicle.VehicleDetailActivity;
import com.projeto.egoodapp.views.vehicle.VehiclesActivity;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private LocalRepository repository;
    private UserChrome chrome;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        repository = LocalRepository.get(this);
        setupActions();
        chrome = new UserChrome(this, UserChrome.Section.HOME);
        renderFeaturedVehicles();
        renderUserAndDealers();
    }

    @Override protected void onResume() {
        super.onResume();
        if (chrome != null) chrome.refresh();
        renderUserAndDealers();
    }

    private void setupActions() {
        findViewById(R.id.quickBtnVehicles).setOnClickListener(v -> open(VehiclesActivity.class));
        findViewById(R.id.quickBtnComparison).setOnClickListener(v -> open(ComparisonActivity.class));
        findViewById(R.id.quickBtnDealerships).setOnClickListener(v -> open(ConcessionariasActivity.class));
        findViewById(R.id.quickBtnSolar).setOnClickListener(v -> open(SolarActivity.class));
        findViewById(R.id.btnSeeAll).setOnClickListener(v -> open(VehiclesActivity.class));
        findViewById(R.id.btnSeeAllDealers).setOnClickListener(v -> open(ConcessionariasActivity.class));
    }

    private void open(Class<?> destination) {
        startActivity(new Intent(this, destination).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }

    private void renderUserAndDealers() {
        AccountProfile profile = LocalSession.current(this, "pessoa");
        if (profile == null) return;
        String fullName = profile.name == null ? "" : profile.name.trim();
        String firstName = fullName.isEmpty() ? "Usuário" : fullName.split("\\s+")[0];
        ((TextView) findViewById(R.id.tvUserGreeting)).setText("Olá, " + firstName);
        ((TextView) findViewById(R.id.tvTimeGreeting)).setText(timeGreeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)));
        renderDealers(repository.dealers());
    }

    static String timeGreeting(int hour) {
        if (hour >= 5 && hour < 12) return "Bom dia!";
        if (hour >= 12 && hour < 18) return "Boa tarde!";
        return "Boa noite!";
    }

    private void renderFeaturedVehicles() {
        LinearLayout container = findViewById(R.id.homeFeaturedList);
        container.removeAllViews();
        addFeatured(container, repository.vehicle("demo-dolphin"), true);
        addFeatured(container, repository.vehicle("demo-ex30"), false);
    }

    private void addFeatured(LinearLayout container, Vehicle vehicle, boolean first) {
        if (vehicle == null) return;
        View card = LayoutInflater.from(this).inflate(R.layout.item_home_featured_vehicle, container, false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        int gap = Math.round(6 * getResources().getDisplayMetrics().density);
        if (first) params.setMarginEnd(gap); else params.setMarginStart(gap);
        card.setLayoutParams(params);
        VehiclePhotos.load(vehicle, (ImageView) card.findViewById(R.id.homeVehiclePhoto));
        text(card, R.id.homeVehicleBadge, vehicle.getBadge());
        text(card, R.id.homeVehicleName, vehicle.getNome());
        text(card, R.id.homeVehicleSpecs, vehicle.getAutonomia() + " km  •  " + vehicle.getBateria() + " kWh");
        text(card, R.id.homeVehiclePrice, currency.format(vehicle.getPreco()));
        card.setContentDescription("Abrir detalhes de " + vehicle.getNome());
        card.setOnClickListener(v -> startActivity(new Intent(this, VehicleDetailActivity.class).putExtra("vehicleId", vehicle.getId())));
        container.addView(card);
    }

    private void renderDealers(List<AccountProfile> dealers) {
        LinearLayout container = findViewById(R.id.homeDealersList);
        View empty = findViewById(R.id.homeDealerEmpty);
        container.removeAllViews();
        empty.setVisibility(dealers.isEmpty() ? View.VISIBLE : View.GONE);
        int count = Math.min(2, dealers.size());
        for (int i = 0; i < count; i++) {
            AccountProfile dealer = dealers.get(i);
            View card = LayoutInflater.from(this).inflate(R.layout.item_home_dealership, container, false);
            String name = dealer.name == null || dealer.name.trim().isEmpty() ? "Concessionária" : dealer.name.trim();
            String city = dealer.city == null ? "" : dealer.city.trim();
            String state = dealer.state == null ? "" : dealer.state.trim();
            String location = city.isEmpty() ? state : state.isEmpty() ? city : city + ", " + state;
            int vehicleCount = repository.dealerVehicles(dealer.uid).size();
            text(card, R.id.homeDealerName, name);
            text(card, R.id.homeDealerMeta, location.isEmpty() ? "Localização não informada" : location);
            text(card, R.id.homeDealerStock, vehicleCount + (vehicleCount == 1 ? " veículo disponível" : " veículos disponíveis"));
            card.setContentDescription("Abrir concessionária " + name);
            card.setOnClickListener(v -> startActivity(new Intent(this, ConcessionariaDetailActivity.class).putExtra("dealerId", dealer.uid)));
            container.addView(card);
        }
    }

    private static void text(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value == null ? "" : value);
    }
}

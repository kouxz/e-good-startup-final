package com.projeto.egoodapp.views.vehicle;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.slider.Slider;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.simulation.SimulationCalculations;
import com.projeto.egoodapp.views.SimulationFormat;
import com.projeto.egoodapp.views.comparison.ComparisonActivity;
import com.projeto.egoodapp.views.user.HomeActivity;
import com.projeto.egoodapp.views.user.ProfileActivity;
import com.projeto.egoodapp.views.user.UserChrome;
import java.math.BigDecimal;

public class SolarActivity extends AppCompatActivity {
    private Slider sliderSystemPower, sliderLightCost, sliderSolarTariff;
    private UserChrome chrome;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solar);
        sliderSystemPower = findViewById(R.id.sliderSystemPower);
        sliderLightCost = findViewById(R.id.sliderLightCost);
        sliderSolarTariff = findViewById(R.id.sliderSolarTariff);
        if (savedInstanceState != null) {
            sliderSystemPower.setValue(savedInstanceState.getFloat("power", 5f));
            sliderLightCost.setValue(savedInstanceState.getFloat("bill", 400f));
            sliderSolarTariff.setValue(savedInstanceState.getFloat("tariff", .85f));
        }
        for (Slider slider : new Slider[]{sliderSystemPower, sliderLightCost, sliderSolarTariff}) {
            slider.addOnChangeListener((s, value, fromUser) -> updateCalculations());
        }
        setupNavigation();
        chrome = new UserChrome(this, UserChrome.Section.SOLAR);
        updateCalculations();
    }

    @Override protected void onResume() {
        super.onResume();
        if (chrome != null) chrome.refresh();
    }

    private void updateCalculations() {
        double power = Math.round(sliderSystemPower.getValue() * 10) / 10.0;
        int bill = Math.round(sliderLightCost.getValue());
        double tariff = Math.round(sliderSolarTariff.getValue() * 100) / 100.0;
        SimulationCalculations.Solar result = SimulationCalculations.solar(power, bill, tariff);
        setText(R.id.tvSystemPowerValue, SimulationFormat.number(power) + " kWp");
        setText(R.id.tvLightCostValue, SimulationFormat.money(BigDecimal.valueOf(bill)));
        setText(R.id.tvSolarTariffValue, SimulationFormat.money(BigDecimal.valueOf(tariff)));
        String generation = SimulationFormat.integer(result.monthlyGeneration) + " kWh";
        String payback = result.paybackYears == null ? "Sem retorno estimado"
                : String.format(SimulationFormat.BRAZIL, "%.1f anos", result.paybackYears);
        setText(R.id.tvMonthlyGenerationTop, generation);
        setText(R.id.tvCO2Top, SimulationFormat.integer(result.annualCO2) + " kg");
        setText(R.id.tvPaybackTop, payback);
        setText(R.id.tvMonthlyGeneration, generation);
        setText(R.id.tvAnnualGeneration, SimulationFormat.integer(result.annualGeneration) + " kWh/ano");
        setText(R.id.tvMonthlySavings, SimulationFormat.money(result.monthlySavings));
        setText(R.id.tvAnnualSolarSavings, SimulationFormat.money(result.annualSavings) + "/ano");
        setText(R.id.tvInvestment, SimulationFormat.money(result.investment));
        setText(R.id.tvPayback, payback);
        setText(R.id.tvRoofEstimate, "Área estimada: ~" + SimulationFormat.number(result.roofArea)
                + " m² · aproximadamente " + result.panels + " painéis de 400 W");
    }

    private void setText(int id, String value) { ((TextView) findViewById(id)).setText(value); }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putFloat("power", sliderSystemPower.getValue());
        out.putFloat("bill", sliderLightCost.getValue());
        out.putFloat("tariff", sliderSolarTariff.getValue());
    }

    private void setupNavigation() {
        LinearLayout btnHome = findViewById(R.id.btnHome);
        LinearLayout btnSolar = findViewById(R.id.btnSolar);
        LinearLayout btnProfile = findViewById(R.id.btnProfile);
        LinearLayout btnComparison = findViewById(R.id.btnComparison);
        LinearLayout btnVehicles = findViewById(R.id.btnVehicles);

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(SolarActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnSolar.setOnClickListener(v -> {
            // Already on Solar
        });

        btnVehicles.setOnClickListener(v -> {
            Intent intent = new Intent(SolarActivity.this, VehiclesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(SolarActivity.this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnComparison.setOnClickListener(v -> {
            Intent intent = new Intent(SolarActivity.this, ComparisonActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }


}

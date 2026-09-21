package com.projeto.egoodapp.views.comparison;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.simulation.SimulationCalculations;
import com.projeto.egoodapp.views.SimulationFormat;
import com.projeto.egoodapp.views.user.HomeActivity;
import com.projeto.egoodapp.views.user.ProfileActivity;
import com.projeto.egoodapp.views.user.UserChrome;
import com.projeto.egoodapp.views.vehicle.SolarActivity;
import com.projeto.egoodapp.views.vehicle.VehiclesActivity;
import java.math.BigDecimal;

public class ComparisonActivity extends AppCompatActivity {
    private Spinner spEvCalculadora, spCombCalculadora, spEvComparacao, spCombComparacao;
    private Slider sliderEnergyCost, sliderGasCost, sliderMonthlyKm;
    private MaterialButtonToggleGroup tabs;
    private boolean calculatorMode;
    private UserChrome chrome;
    private static final String[] ELECTRIC_VEHICLES = {
        "BYD Dolphin Mini — 12,9 kWh/100km",
        "BYD Dolphin — 13,4 kWh/100km",
        "Volvo EX30 — 15,1 kWh/100km",
        "Mercedes EQE SUV — 18,9 kWh/100km",
        "Fiat 500e — 14,2 kWh/100km",
        "Chevrolet Bolt EV — 15,6 kWh/100km"
    };

    private static final double[] EV_CONSUMPTION = { 12.9, 13.4, 15.1, 18.9, 14.2, 15.6 };
    private static final int[] EV_PRICES = { 119800, 149800, 285000, 729900, 189990, 199990 };

    private static final String[] COMBUSTION_VEHICLES = {
        "Chevrolet Onix 1.0 Turbo — 12,5 L/100km",
        "Renault Sandero 1.0 — 12,3 L/100km",
        "VW Polo 1.0 — 11,8 L/100km",
        "Toyota Corolla 2.0 — 10,2 L/100km",
        "Honda HR-V 1.5 Turbo — 13,1 L/100km"
    };

    private static final double[] COMBUSTION_CONSUMPTION = { 12.5, 12.3, 11.8, 10.2, 13.1 };
    private static final int[] COMBUSTION_PRICES = { 89990, 79990, 105990, 145990, 159990 };


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comparison);
        spEvCalculadora = findViewById(R.id.spEvCalculadora);
        spCombCalculadora = findViewById(R.id.spCombCalculadora);
        spEvComparacao = findViewById(R.id.spEvComparacao);
        spCombComparacao = findViewById(R.id.spCombComparacao);
        sliderEnergyCost = findViewById(R.id.sliderEnergyCost);
        sliderGasCost = findViewById(R.id.sliderGasCost);
        sliderMonthlyKm = findViewById(R.id.sliderMonthlyKm);
        tabs = findViewById(R.id.comparisonTabs);
        setupSpinners();
        if (savedInstanceState != null) {
            spEvCalculadora.setSelection(savedInstanceState.getInt("calcEv", 0));
            spCombCalculadora.setSelection(savedInstanceState.getInt("calcComb", 0));
            spEvComparacao.setSelection(savedInstanceState.getInt("compareEv", 3));
            spCombComparacao.setSelection(savedInstanceState.getInt("compareComb", 1));
            sliderEnergyCost.setValue(savedInstanceState.getFloat("energy", .85f));
            sliderGasCost.setValue(savedInstanceState.getFloat("gas", 5.79f));
            sliderMonthlyKm.setValue(savedInstanceState.getFloat("km", 1500f));
            calculatorMode = savedInstanceState.getBoolean("calculatorMode");
        }
        for (Slider slider : new Slider[]{sliderEnergyCost, sliderGasCost, sliderMonthlyKm}) {
            slider.addOnChangeListener((s, value, fromUser) -> updateCalculator());
        }
        tabs.addOnButtonCheckedListener((group, checkedId, checked) -> {
            if (checked) showMode(checkedId == R.id.tabCalculadora);
        });
        tabs.check(calculatorMode ? R.id.tabCalculadora : R.id.tabComparacao);
        showMode(calculatorMode);
        setupNavigation();
        chrome = new UserChrome(this, UserChrome.Section.COMPARISON);
        updateCalculator();
        updateComparacao();
    }

    @Override protected void onResume() {
        super.onResume();
        if (chrome != null) chrome.refresh();
    }

    private void setupSpinners() {
        ArrayAdapter<String> ev = new ArrayAdapter<>(this, R.layout.item_simulation_model, ELECTRIC_VEHICLES);
        ev.setDropDownViewResource(R.layout.item_simulation_dropdown);
        ArrayAdapter<String> combustion = new ArrayAdapter<>(this, R.layout.item_simulation_model, COMBUSTION_VEHICLES);
        combustion.setDropDownViewResource(R.layout.item_simulation_dropdown);
        spEvCalculadora.setAdapter(ev);
        spEvComparacao.setAdapter(ev);
        spCombCalculadora.setAdapter(combustion);
        spCombComparacao.setAdapter(combustion);
        spEvComparacao.setSelection(3);
        spCombComparacao.setSelection(1);
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateCalculator();
                updateComparacao();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        for (Spinner spinner : new Spinner[]{spEvCalculadora, spCombCalculadora, spEvComparacao, spCombComparacao}) {
            spinner.setOnItemSelectedListener(listener);
        }
    }

    private void showMode(boolean calculator) {
        calculatorMode = calculator;
        findViewById(R.id.containerComparacao).setVisibility(calculator ? View.GONE : View.VISIBLE);
        findViewById(R.id.containerCalculadora).setVisibility(calculator ? View.VISIBLE : View.GONE);
        setText(R.id.tvTitleComparison, calculator ? "Comparação de Custo Mensal" : "Comparação Detalhada");
        setText(R.id.tvSubtitleComparison, calculator
                ? "Ajuste os valores para simular seus gastos mensais"
                : "Escolha dois veículos para comparar");
    }

    private void updateCalculator() {
        int ev = spEvCalculadora.getSelectedItemPosition();
        int combustion = spCombCalculadora.getSelectedItemPosition();
        if (ev < 0 || combustion < 0) return;
        // Sliders use floats; recover exact hundredths before monetary arithmetic.
        double energy = Math.round(sliderEnergyCost.getValue() * 100) / 100.0;
        double gas = Math.round(sliderGasCost.getValue() * 100) / 100.0;
        int distance = Math.round(sliderMonthlyKm.getValue());
        setText(R.id.tvEnergyCostValue, SimulationFormat.money(BigDecimal.valueOf(energy)));
        setText(R.id.tvGasCostValue, SimulationFormat.money(BigDecimal.valueOf(gas)));
        setText(R.id.tvMonthlyKmValue, SimulationFormat.integer(distance) + " km");
        setVehicleData(ev, combustion, true);
        SimulationCalculations.Comparison result = SimulationCalculations.compare(
                EV_CONSUMPTION[ev], COMBUSTION_CONSUMPTION[combustion], distance, energy, gas);
        setText(R.id.tvCostEvCalc, SimulationFormat.money(result.electricCost));
        setText(R.id.tvCostCombCalc, SimulationFormat.money(result.combustionCost));
        boolean additional = result.difference.signum() < 0;
        setText(R.id.tvMonthlySavingsLabel, additional ? "Custo adicional mensal" : "Economia mensal");
        setText(R.id.tvAnnualSavingsCalcLabel, additional ? "Custo adicional anual" : "Economia anual estimada");
        setText(R.id.tvMonthlySavings, SimulationFormat.money(result.difference.abs()));
        setText(R.id.tvAnnualSavingsCalc, SimulationFormat.money(result.annualDifference().abs()));
        ((TextView) findViewById(R.id.tvAnnualSavingsCalc)).setTextColor(android.graphics.Color.parseColor(additional ? "#C2410C" : "#0F766E"));
        String percentage = result.combustionCost.signum() == 0 ? "Sem base percentual"
                : result.difference.signum() == 0 ? "Mesmo custo"
                : SimulationFormat.integer(Math.abs(result.differencePercent)) + (additional ? "% mais" : "% menos");
        setText(R.id.tvSavingsPercent, percentage);
    }

    private void updateComparacao() {
        int ev = spEvComparacao.getSelectedItemPosition();
        int combustion = spCombComparacao.getSelectedItemPosition();
        if (ev < 0 || combustion < 0) return;
        setVehicleData(ev, combustion, false);
        SimulationCalculations.Comparison result = SimulationCalculations.compare(
                EV_CONSUMPTION[ev], COMBUSTION_CONSUMPTION[combustion], 15000, .85, 5.79);
        setText(R.id.tvCostEvComparacao, SimulationFormat.money(result.electricCost));
        setText(R.id.tvCostCombComparacao, SimulationFormat.money(result.combustionCost));
        setText(R.id.tvAnnualSavings, SimulationFormat.money(result.difference.abs()));
        setText(R.id.tvAnnualSavingsLabel, result.difference.signum() < 0
                ? "Custo adicional anual com o elétrico" : "Economia anual estimada com o elétrico");
    }

    private void setVehicleData(int ev, int combustion, boolean calculator) {
        setText(calculator ? R.id.tvConsumEvCalc : R.id.tvConsumEvComparacao,
                SimulationFormat.number(EV_CONSUMPTION[ev]) + " kWh/100 km");
        setText(calculator ? R.id.tvConsumCombCalc : R.id.tvConsumCombComparacao,
                SimulationFormat.number(COMBUSTION_CONSUMPTION[combustion]) + " L/100 km");
        setText(calculator ? R.id.tvPriceEvCalc : R.id.tvPriceEvComparacao,
                SimulationFormat.money(BigDecimal.valueOf(EV_PRICES[ev])));
        setText(calculator ? R.id.tvPriceCombCalc : R.id.tvPriceCombComparacao,
                SimulationFormat.money(BigDecimal.valueOf(COMBUSTION_PRICES[combustion])));
    }

    private void setText(int id, String value) { ((TextView) findViewById(id)).setText(value); }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("calculatorMode", calculatorMode);
        out.putInt("calcEv", spEvCalculadora.getSelectedItemPosition());
        out.putInt("calcComb", spCombCalculadora.getSelectedItemPosition());
        out.putInt("compareEv", spEvComparacao.getSelectedItemPosition());
        out.putInt("compareComb", spCombComparacao.getSelectedItemPosition());
        out.putFloat("energy", sliderEnergyCost.getValue());
        out.putFloat("gas", sliderGasCost.getValue());
        out.putFloat("km", sliderMonthlyKm.getValue());
    }

    private void setupNavigation() {
        LinearLayout btnHome = findViewById(R.id.btnHome);
        LinearLayout btnComparisonNav = findViewById(R.id.btnComparison);
        LinearLayout btnSolar = findViewById(R.id.btnSolar);
        LinearLayout btnVehicles = findViewById(R.id.btnVehicles);
        LinearLayout btnProfile = findViewById(R.id.btnProfile);

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(ComparisonActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnComparisonNav.setOnClickListener(v -> {
            // Já está na tela de comparação
        });

        btnSolar.setOnClickListener(v -> {
            Intent intent = new Intent(ComparisonActivity.this, SolarActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnVehicles.setOnClickListener(v -> {
            Intent intent = new Intent(ComparisonActivity.this, VehiclesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ComparisonActivity.this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }


}

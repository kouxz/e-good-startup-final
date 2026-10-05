package com.projeto.egoodapp.views.comparison;

import android.view.View;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.views.comparison.ComparisonActivity;
import com.projeto.egoodapp.views.solar.SolarActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SimulationScreensTest {
    @org.junit.Before public void requireAuthenticatedSession() {
        org.junit.Assume.assumeTrue("Simulation screens require a test account signed in",
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null);
    }
    @Test public void comparisonKeepsBothSelectionsAndCalculatorValuesAfterRecreation() {
        try (ActivityScenario<ComparisonActivity> scenario = ActivityScenario.launch(ComparisonActivity.class)) {
            scenario.onActivity(activity -> {
                assertEquals("R$ 164,48", ((TextView) activity.findViewById(R.id.tvCostEvCalc)).getText().toString());
                assertTrue(activity.findViewById(R.id.ivComparisonElectricIcon) instanceof ImageView);
                assertTrue(activity.findViewById(R.id.ivComparisonCombustionIcon) instanceof ImageView);
                assertTrue(activity.findViewById(R.id.ivCalculatorElectricIcon) instanceof ImageView);
                assertTrue(activity.findViewById(R.id.ivCalculatorCombustionIcon) instanceof ImageView);
                ((Spinner) activity.findViewById(R.id.spEvComparacao)).setSelection(2);
                ((Spinner) activity.findViewById(R.id.spCombComparacao)).setSelection(3);
                ((Spinner) activity.findViewById(R.id.spEvCalculadora)).setSelection(1);
                ((Spinner) activity.findViewById(R.id.spCombCalculadora)).setSelection(2);
                ((Slider) activity.findViewById(R.id.sliderEnergyCost)).setValue(1.25f);
                ((Slider) activity.findViewById(R.id.sliderGasCost)).setValue(6.50f);
                ((Slider) activity.findViewById(R.id.sliderMonthlyKm)).setValue(2500f);
                ((MaterialButtonToggleGroup) activity.findViewById(R.id.comparisonTabs)).check(R.id.tabCalculadora);
                assertEquals(View.VISIBLE, activity.findViewById(R.id.containerCalculadora).getVisibility());
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals(R.id.tabCalculadora, ((MaterialButtonToggleGroup) activity.findViewById(R.id.comparisonTabs)).getCheckedButtonId());
                assertEquals(2, ((Spinner) activity.findViewById(R.id.spEvComparacao)).getSelectedItemPosition());
                assertEquals(3, ((Spinner) activity.findViewById(R.id.spCombComparacao)).getSelectedItemPosition());
                assertEquals(1, ((Spinner) activity.findViewById(R.id.spEvCalculadora)).getSelectedItemPosition());
                assertEquals(2, ((Spinner) activity.findViewById(R.id.spCombCalculadora)).getSelectedItemPosition());
                assertEquals(1.25f, ((Slider) activity.findViewById(R.id.sliderEnergyCost)).getValue(), 0);
                assertEquals(6.50f, ((Slider) activity.findViewById(R.id.sliderGasCost)).getValue(), 0);
                assertEquals(2500f, ((Slider) activity.findViewById(R.id.sliderMonthlyKm)).getValue(), 0);
                assertEquals("R$ 418,75", ((TextView) activity.findViewById(R.id.tvCostEvCalc)).getText().toString());
                ((MaterialButtonToggleGroup) activity.findViewById(R.id.comparisonTabs)).check(R.id.tabComparacao);
                assertEquals(View.VISIBLE, activity.findViewById(R.id.containerComparacao).getVisibility());
            });
        }
    }

    @Test public void solarControlsAndResultsSurviveRecreation() {
        try (ActivityScenario<SolarActivity> scenario = ActivityScenario.launch(SolarActivity.class)) {
            scenario.onActivity(activity -> {
                assertEquals("650 kWh", ((TextView) activity.findViewById(R.id.tvMonthlyGeneration)).getText().toString());
                assertEquals("4,2 anos", ((TextView) activity.findViewById(R.id.tvPayback)).getText().toString());
                ((Slider) activity.findViewById(R.id.sliderSystemPower)).setValue(7.5f);
                ((Slider) activity.findViewById(R.id.sliderLightCost)).setValue(800f);
                ((Slider) activity.findViewById(R.id.sliderSolarTariff)).setValue(.40f);
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals(7.5f, ((Slider) activity.findViewById(R.id.sliderSystemPower)).getValue(), 0);
                assertEquals(800f, ((Slider) activity.findViewById(R.id.sliderLightCost)).getValue(), 0);
                assertEquals(.40f, ((Slider) activity.findViewById(R.id.sliderSolarTariff)).getValue(), 0);
                assertEquals("975 kWh", ((TextView) activity.findViewById(R.id.tvMonthlyGeneration)).getText().toString());
                assertEquals("R$ 390,00", ((TextView) activity.findViewById(R.id.tvMonthlySavings)).getText().toString());
                assertEquals("R$ 4.680,00/ano", ((TextView) activity.findViewById(R.id.tvAnnualSolarSavings)).getText().toString());
                assertTrue(((TextView) activity.findViewById(R.id.tvRoofEstimate)).getText().toString().contains("19 painéis"));
            });
        }
    }
}

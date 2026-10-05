package com.projeto.egoodapp.data.simulation;

import com.projeto.egoodapp.data.simulation.SimulationFormat;
import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;

public class SimulationCalculationsTest {
    private void money(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test public void calculatorDefaultsKeepPrecisionUntilDisplay() {
        SimulationCalculations.Comparison result = SimulationCalculations.compare(12.9, 12.5, 1500, .85, 5.79);
        money("164.475", result.electricCost);
        money("1085.625", result.combustionCost);
        money("921.15", result.difference);
        money("11053.80", result.annualDifference());
        assertEquals("R$ 164,48", SimulationFormat.money(result.electricCost));
        assertEquals("R$ 1.085,63", SimulationFormat.money(result.combustionCost));
        assertEquals("R$ 11.053,80", SimulationFormat.money(result.annualDifference()));
        assertEquals(85, Math.round(result.differencePercent));
    }

    @Test public void annualComparisonUsesDisplayedAssumptions() {
        SimulationCalculations.Comparison result = SimulationCalculations.compare(18.9, 12.3, 15000, .85, 5.79);
        money("2409.75", result.electricCost);
        money("10682.55", result.combustionCost);
        money("8272.80", result.difference);
    }

    @Test public void calculatorAcceptsControlExtremes() {
        SimulationCalculations.Comparison low = SimulationCalculations.compare(12.9, 12.5, 500, .4, 4);
        money("25.8", low.electricCost);
        money("250", low.combustionCost);
        SimulationCalculations.Comparison high = SimulationCalculations.compare(12.9, 12.5, 5000, 2, 9);
        money("1290", high.electricCost);
        money("5625", high.combustionCost);
    }

    @Test public void additionalCostIsPreservedAndZeroBaselineIsSafe() {
        SimulationCalculations.Comparison expensive = SimulationCalculations.compare(30, 5, 1000, 2, 4);
        money("-400", expensive.difference);
        assertEquals(-200, expensive.differencePercent, .00001);
        SimulationCalculations.Comparison zero = SimulationCalculations.compare(10, 10, 1000, 1, 0);
        money("-100", zero.difference);
        assertEquals(0, zero.differencePercent, 0);
    }

    @Test public void solarDefaultsMatchTheIllustrativeModel() {
        SimulationCalculations.Solar result = SimulationCalculations.solar(5, 400, .85);
        assertEquals(650, result.monthlyGeneration, 0);
        assertEquals(7800, result.annualGeneration, 0);
        assertEquals(585, result.annualCO2, 0);
        money("400", result.monthlySavings);
        money("4800", result.annualSavings);
        money("20000", result.investment);
        assertEquals(4.166666666667, result.paybackYears, .000000001);
        assertEquals(13, result.panels);
        assertEquals(30, result.roofArea, 0);
    }

    @Test public void solarSavingsAreLimitedByGenerationAndBill() {
        SimulationCalculations.Solar generationLimited = SimulationCalculations.solar(1, 2000, .4);
        money("52", generationLimited.monthlySavings);
        assertEquals(3, generationLimited.panels);
        SimulationCalculations.Solar billLimited = SimulationCalculations.solar(20, 100, 2);
        money("100", billLimited.monthlySavings);
        assertEquals(50, billLimited.panels);
        assertEquals(120, billLimited.roofArea, 0);
    }

    @Test public void solarReactsToTariffWithoutChangingGeneration() {
        SimulationCalculations.Solar result = SimulationCalculations.solar(5, 2000, 2);
        money("1300", result.monthlySavings);
        assertEquals(650, result.monthlyGeneration, 0);
    }

    @Test public void solarDoesNotShowInfinityWhenThereIsNoEconomy() {
        assertNull(SimulationCalculations.solar(5, 0, .85).paybackYears);
        assertNull(SimulationCalculations.solar(5, 400, 0).paybackYears);
        SimulationCalculations.Solar zero = SimulationCalculations.solar(0, 400, .85);
        assertNull(zero.paybackYears);
        money("0", zero.monthlySavings);
        assertEquals(0, zero.panels);
    }

    @Test(expected = IllegalArgumentException.class) public void negativeInputIsRejected() {
        SimulationCalculations.solar(-1, 400, .85);
    }

    @Test(expected = IllegalArgumentException.class) public void nonFiniteInputIsRejected() {
        SimulationCalculations.compare(12.9, 12.5, Double.NaN, .85, 5.79);
    }
}

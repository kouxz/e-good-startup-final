package com.projeto.egoodapp.data.simulation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Illustrative estimates. Values are rounded only when presented to the user. */
public final class SimulationCalculations {
    public static final double GENERATION_PER_KWP = 130;
    public static final double INSTALLATION_PER_KWP = 4000;
    public static final double CO2_PER_KWH = 0.075;
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    private SimulationCalculations() {}

    public static Comparison compare(double electricConsumption, double combustionConsumption,
                                     double distance, double energyPrice, double gasPrice) {
        BigDecimal electric = cost(electricConsumption, distance, energyPrice);
        BigDecimal combustion = cost(combustionConsumption, distance, gasPrice);
        return new Comparison(electric, combustion);
    }

    private static BigDecimal cost(double consumption, double distance, double price) {
        return nonNegative(consumption).multiply(nonNegative(distance))
                .multiply(nonNegative(price)).movePointLeft(2);
    }

    public static Solar solar(double power, double monthlyBill, double tariff) {
        BigDecimal kwp = nonNegative(power);
        BigDecimal generation = kwp.multiply(BigDecimal.valueOf(GENERATION_PER_KWP));
        BigDecimal savings = generation.multiply(nonNegative(tariff)).min(nonNegative(monthlyBill));
        BigDecimal investment = kwp.multiply(BigDecimal.valueOf(INSTALLATION_PER_KWP));
        return new Solar(kwp, generation, savings, investment);
    }

    private static BigDecimal nonNegative(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid simulation input");
        return BigDecimal.valueOf(value);
    }

    public static final class Comparison {
        public final BigDecimal electricCost, combustionCost, difference;
        public final double differencePercent;

        private Comparison(BigDecimal electric, BigDecimal combustion) {
            electricCost = electric;
            combustionCost = combustion;
            difference = combustion.subtract(electric);
            differencePercent = combustion.signum() == 0 ? 0 : difference
                    .multiply(BigDecimal.valueOf(100)).divide(combustion, 12, RoundingMode.HALF_UP).doubleValue();
        }

        public BigDecimal annualDifference() { return difference.multiply(TWELVE); }
    }

    public static final class Solar {
        public final double monthlyGeneration, annualGeneration, annualCO2, roofArea;
        public final int panels;
        public final BigDecimal monthlySavings, annualSavings, investment;
        public final Double paybackYears;

        private Solar(BigDecimal power, BigDecimal generation, BigDecimal savings, BigDecimal cost) {
            monthlyGeneration = generation.doubleValue();
            annualGeneration = generation.multiply(TWELVE).doubleValue();
            annualCO2 = generation.multiply(TWELVE).multiply(BigDecimal.valueOf(CO2_PER_KWH)).doubleValue();
            roofArea = power.multiply(BigDecimal.valueOf(6)).doubleValue();
            panels = power.multiply(BigDecimal.valueOf(1000)).divide(BigDecimal.valueOf(400),
                    0, RoundingMode.CEILING).intValueExact();
            monthlySavings = savings;
            annualSavings = savings.multiply(TWELVE);
            investment = cost;
            paybackYears = annualSavings.signum() == 0 ? null : cost
                    .divide(annualSavings, 12, RoundingMode.HALF_UP).doubleValue();
        }
    }
}

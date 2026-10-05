package com.projeto.egoodapp.data.simulation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class SimulationFormat {
    public static final Locale BRAZIL = new Locale("pt", "BR");
    private SimulationFormat() {}

    public static String money(BigDecimal value) {
        return "R$ " + decimal("#,##0.00").format(value.setScale(2, RoundingMode.HALF_UP));
    }

    public static String number(double value) { return decimal("#,##0.#").format(value); }
    public static String integer(double value) { return decimal("#,##0").format(value); }

    private static DecimalFormat decimal(String pattern) {
        DecimalFormat format = new DecimalFormat(pattern, new DecimalFormatSymbols(BRAZIL));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }
}

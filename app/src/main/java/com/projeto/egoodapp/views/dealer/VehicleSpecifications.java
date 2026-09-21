package com.projeto.egoodapp.views.dealer;

public final class VehicleSpecifications {
    public static double consumption(String value) {
        double parsed = Double.parseDouble(value.trim().replace(',', '.'));
        if (!Double.isFinite(parsed) || parsed <= 0) throw new IllegalArgumentException("Consumo inválido");
        return parsed;
    }
    public static int power(String value) {
        int parsed = Integer.parseInt(value.trim());
        if (parsed <= 0) throw new IllegalArgumentException("Potência inválida");
        return parsed;
    }
    public static String charging(String value) {
        String parsed = value == null ? "" : value.trim();
        if (parsed.isEmpty()) throw new IllegalArgumentException("Informe a recarga");
        return parsed;
    }
    private VehicleSpecifications() {}
}

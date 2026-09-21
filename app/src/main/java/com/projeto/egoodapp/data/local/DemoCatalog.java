package com.projeto.egoodapp.data.local;

import com.projeto.egoodapp.models.Vehicle;
import java.util.List;

public final class DemoCatalog {
    private DemoCatalog() {}
    public static List<Vehicle> vehicles() {
        return java.util.Arrays.asList(
            car("mini", "BYD", "Dolphin Mini", "Hatch", 119800, 12.9, 30, 240, 100, "AC", "car_byd_dolphin_mini", "Mais vendido"),
            car("dolphin", "BYD", "Dolphin", "Hatch", 149800, 13.4, 38, 305, 101, "AC", "car_byd_dolphin", "Popular"),
            car("ex30", "Volvo", "EX30", "SUV", 285000, 15.1, 40, 265, 170, "AC/DC", "car_volvo_ex30", "Premium"),
            car("eqe", "Mercedes", "EQE SUV", "Luxo", 729900, 18.9, 53, 280, 260, "AC/DC", "car_eqe_suv", "Luxo"),
            car("ex2", "Geely", "EX2", "Hatch", 123800, 14.2, 42, 320, 118, "AC 11 kW", "car_geely_ex2", "Novo"),
            car("bolt", "Chevrolet", "Bolt EV", "Hatch", 199990, 15.6, 65, 417, 200, "AC/DC", "car_bolt_ev", "Destaque"));
    }
    private static Vehicle car(String id, String brand, String model, String category, double price,
            double consumption, int battery, int autonomy, int power, String charge, String image, String badge) {
        Vehicle v = new Vehicle(brand, model, 2024, 0, price, category, battery, autonomy, "", "", "");
        v.setId("demo-" + id); v.setConsumo(consumption); v.setPotencia(power); v.setCarga(charge);
        v.setImageName(image); v.setBadge(badge);
        return v;
    }
}

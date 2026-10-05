package com.projeto.egoodapp.data.simulation;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import com.projeto.egoodapp.data.simulation.SimulationFormat;

/** The illustrative models shared by the comparison screen and the local assistant. */
public final class SimulationCatalog {
    private SimulationCatalog() {}
    public static final List<Model> ELECTRIC = Collections.unmodifiableList(Arrays.asList(
            new Model("demo-mini", "BYD Dolphin Mini", 12.9, 119800),
            new Model("demo-dolphin", "BYD Dolphin", 13.4, 149800),
            new Model("demo-ex30", "Volvo EX30", 15.1, 285000),
            new Model("demo-eqe", "Mercedes EQE SUV", 18.9, 729900),
            new Model("demo-500e", "Fiat 500e", 14.2, 189990),
            new Model("demo-bolt", "Chevrolet Bolt EV", 15.6, 199990)));
    public static final List<Model> COMBUSTION = Collections.unmodifiableList(Arrays.asList(
            new Model("comb-onix", "Chevrolet Onix 1.0 Turbo", 12.5, 89990),
            new Model("comb-sandero", "Renault Sandero 1.0", 12.3, 79990),
            new Model("comb-polo", "VW Polo 1.0", 11.8, 105990),
            new Model("comb-corolla", "Toyota Corolla 2.0", 10.2, 145990),
            new Model("comb-hrv", "Honda HR-V 1.5 Turbo", 13.1, 159990)));

    public static String[] labels(boolean electric) {
        return (electric ? ELECTRIC : COMBUSTION).stream()
                .map(model -> model.name + " — " + SimulationFormat.number(model.consumption)
                        + (electric ? " kWh/100km" : " L/100km"))
                .toArray(String[]::new);
    }

    public static final class Model {
        public final String id, name;
        public final double consumption;
        public final int price;
        private Model(String id, String name, double consumption, int price) {
            this.id = id; this.name = name; this.consumption = consumption; this.price = price;
        }
    }
}

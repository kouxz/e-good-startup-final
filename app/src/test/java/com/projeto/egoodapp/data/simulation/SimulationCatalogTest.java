package com.projeto.egoodapp.data.simulation;

import org.junit.Test;
import java.util.HashSet;
import static org.junit.Assert.*;

public class SimulationCatalogTest {
    @Test public void catalogPreservesScreenOrderPricesAndConsumptionLabels() {
        assertArrayEquals(new String[]{"BYD Dolphin Mini — 12,9 kWh/100km", "BYD Dolphin — 13,4 kWh/100km",
                "Volvo EX30 — 15,1 kWh/100km", "Mercedes EQE SUV — 18,9 kWh/100km", "Fiat 500e — 14,2 kWh/100km",
                "Chevrolet Bolt EV — 15,6 kWh/100km"}, SimulationCatalog.labels(true));
        assertArrayEquals(new String[]{"Chevrolet Onix 1.0 Turbo — 12,5 L/100km", "Renault Sandero 1.0 — 12,3 L/100km",
                "VW Polo 1.0 — 11,8 L/100km", "Toyota Corolla 2.0 — 10,2 L/100km", "Honda HR-V 1.5 Turbo — 13,1 L/100km"}, SimulationCatalog.labels(false));
        assertArrayEquals(new int[]{119800, 149800, 285000, 729900, 189990, 199990}, SimulationCatalog.ELECTRIC.stream().mapToInt(m -> m.price).toArray());
        assertArrayEquals(new int[]{89990, 79990, 105990, 145990, 159990}, SimulationCatalog.COMBUSTION.stream().mapToInt(m -> m.price).toArray());
        var ids = new HashSet<String>();
        for (var list : java.util.List.of(SimulationCatalog.ELECTRIC, SimulationCatalog.COMBUSTION))
            for (var model : list) assertTrue(ids.add(model.id));
    }
}

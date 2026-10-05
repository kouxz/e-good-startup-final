package com.projeto.egoodapp.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.model.Vehicle;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class DemoCatalogTest {
    @Test
    public void stableIdsResolveToTheirOwnVehicleImages() {
        Map<String, Vehicle> byId = new HashMap<>();
        for (Vehicle vehicle : DemoCatalog.vehicles()) byId.put(vehicle.getId(), vehicle);

        assertEquals("BYD Dolphin Mini", byId.get("demo-mini").getNome());
        assertEquals("car_byd_dolphin_mini", byId.get("demo-mini").getImageName());
        assertEquals(R.drawable.car_byd_dolphin_mini, DemoCatalog.imageResource("demo-mini"));

        assertEquals("BYD Dolphin", byId.get("demo-dolphin").getNome());
        assertEquals("car_byd_dolphin", byId.get("demo-dolphin").getImageName());
        assertEquals(R.drawable.car_byd_dolphin, DemoCatalog.imageResource("demo-dolphin"));

        assertEquals("Volvo EX30", byId.get("demo-ex30").getNome());
        assertEquals("car_volvo_ex30", byId.get("demo-ex30").getImageName());
        assertEquals(R.drawable.car_volvo_ex30, DemoCatalog.imageResource("demo-ex30"));

        assertEquals(R.drawable.car_eqe_suv, DemoCatalog.imageResource("demo-eqe"));
        assertEquals(R.drawable.car_geely_ex2, DemoCatalog.imageResource("demo-ex2"));
        assertEquals(R.drawable.car_bolt_ev, DemoCatalog.imageResource("demo-bolt"));

        for (Vehicle vehicle : DemoCatalog.vehicles()) {
            assertNotEquals(0, DemoCatalog.imageResource(vehicle.getId()));
        }
        assertEquals(0, DemoCatalog.imageResource("vehicle-from-dealer"));
        assertEquals(0, DemoCatalog.imageResource(null));
    }

    @Test
    public void searchAndCategoryRemainCombined() {
        java.util.List<Vehicle> dolphins = VehicleCatalog.filter(
                DemoCatalog.vehicles(), "Hatch", "dolphin", null);
        assertEquals(2, dolphins.size());
        assertTrue(dolphins.stream().allMatch(vehicle -> "Hatch".equals(vehicle.getCategoria())));

        java.util.List<Vehicle> ex30 = VehicleCatalog.filter(
                DemoCatalog.vehicles(), "SUV", "volvo ex30", null);
        assertEquals(1, ex30.size());
        assertEquals("demo-ex30", ex30.get(0).getId());

        assertTrue(VehicleCatalog.filter(
                DemoCatalog.vehicles(), "Luxo", "dolphin", null).isEmpty());
    }
}

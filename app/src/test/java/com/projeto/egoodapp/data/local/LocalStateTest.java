package com.projeto.egoodapp.data.local;

import com.google.gson.Gson;
import com.projeto.egoodapp.models.Vehicle;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LocalStateTest {
    private LocalState state;
    private Vehicle vehicle;
    private final Gson gson = new Gson();
    @Before public void setup() {
        state = new LocalState();
        state.saveAccount(profile("dealer-a", "concessionaria"));
        state.saveAccount(profile("dealer-b", "concessionaria"));
        state.saveAccount(profile("user", "pessoa"));
        vehicle = new Vehicle("BYD", "Seal", 2024, 0, 200000, "Luxo", 60, 450, "Branco", "Descrição", "file:///foto.img");
        vehicle.setConcessionariaId("dealer-a"); state.vehicles.add(vehicle);
    }
    static AccountProfile profile(String id, String type) {
        AccountProfile p = new AccountProfile(); p.uid = id; p.type = type; p.name = id;
        p.phone = "(11) 99999-1234"; p.email = id + "@example.com";
        p.cnpj = "12.345.678/0001-99"; p.address = "Rua 1"; p.city = "Sorocaba"; p.state = "SP";
        return p;
    }
    private LocalState restart() { return gson.fromJson(gson.toJson(state), LocalState.class); }
    @Test public void migrationBelongsToFirstDealerAndIsNotRepeatedAfterRestart() {
        vehicle.setConcessionariaId(null); state.legacyAssigned = false;
        assertFalse(state.assignLegacy("user"));
        assertTrue(state.assignLegacy("dealer-a"));
        state = restart();
        assertFalse(state.assignLegacy("dealer-b"));
        assertEquals(1, state.dealerVehicles("dealer-a").size());
        assertTrue(state.dealerVehicles("dealer-b").isEmpty());
    }
    @Test public void deletionIsRestrictedToOwnerAndPersistsWithoutDeletingOtherStock() {
        Vehicle second = new Vehicle(); second.setId("second"); second.setConcessionariaId("dealer-b"); state.vehicles.add(second);
        assertFalse(state.deleteVehicle("dealer-b", vehicle.getId()));
        assertFalse(state.deleteVehicle("user", vehicle.getId()));
        assertTrue(state.deleteVehicle("dealer-a", vehicle.getId()));
        state = restart();
        assertTrue(state.dealerVehicles("dealer-a").isEmpty());
        assertEquals("second", state.dealerVehicles("dealer-b").get(0).getId());
    }
    @Test public void duplicateInterestKeepsDateAndStatusWhileGeneralInterestIsSeparate() {
        assertTrue(state.addInterest("user", "dealer-a", vehicle));
        Interest lead = state.dealerInterests("dealer-a").get(0); long date = lead.createdAt;
        assertTrue(state.updateStatus("dealer-a", lead.id, "Em contato"));
        state = restart();
        assertFalse(state.addInterest("user", "dealer-a", state.vehicles.get(0)));
        assertEquals(date, state.interests.get(0).createdAt);
        assertEquals("Em contato", state.interests.get(0).status);
        assertTrue(state.addInterest("user", "dealer-a", null));
        assertFalse(state.addInterest("user", "dealer-a", null));
        assertEquals(2, state.interests.size());
    }
    @Test public void deletingVehiclePreservesContactAndVehicleName() {
        state.addInterest("user", "dealer-a", vehicle);
        state.deleteVehicle("dealer-a", vehicle.getId()); state = restart();
        Interest lead = state.dealerInterests("dealer-a").get(0);
        assertEquals("BYD Seal", lead.vehicleName);
        assertEquals("user", lead.name); assertEquals("(11) 99999-1234", lead.phone);
        assertTrue(state.dealerInterests("dealer-b").isEmpty());
        assertFalse(state.updateStatus("dealer-b", lead.id, "Finalizado"));
        assertTrue(state.updateStatus("dealer-a", lead.id, "Finalizado"));
        assertFalse(state.updateStatus("dealer-a", lead.id, "Inválido"));
    }
    @Test(expected = IllegalArgumentException.class) public void incompleteUserMustCompletePhone() {
        state.account("user").phone = ""; state.addInterest("user", "dealer-a", vehicle);
    }
    @Test(expected = IllegalArgumentException.class) public void interestCannotBeRoutedToAnotherVehicleOwner() {
        state.addInterest("user", "dealer-b", vehicle);
    }
    @Test(expected = IllegalArgumentException.class) public void deletedVehicleCannotReceiveNewInterest() {
        state.vehicles.clear(); state.addInterest("user", "dealer-a", vehicle);
    }
    @Test public void searchCombinesBrandCategoryAndDealer() {
        state.vehicles.addAll(DemoCatalog.vehicles());
        assertEquals(1, VehicleCatalog.filter(state.vehicles, "Luxo", "  byd SEAL  ", "dealer-a").size());
        assertTrue(VehicleCatalog.filter(state.vehicles, "Hatch", "Seal", null).isEmpty());
        assertTrue(VehicleCatalog.filter(state.vehicles, "Todos", "Seal", "dealer-b").isEmpty());
        assertEquals(6, VehicleCatalog.filter(DemoCatalog.vehicles(), "Todos", "", null).size());
    }
    @Test public void legacyJsonKeepsFieldsAndMissingSpecsRemainUnknown() {
        Vehicle old = gson.fromJson("{\"id\":\"old-id\",\"marca\":\"BYD\",\"modelo\":\"Seal\",\"preco\":199900,\"imagemUrl\":\"content://photo/1\"}", Vehicle.class);
        state.vehicles.clear(); state.vehicles.add(old); state.assignLegacy("dealer-a"); state = restart();
        assertEquals("old-id", state.vehicles.get(0).getId());
        assertEquals(199900, state.vehicles.get(0).getPreco(), 0);
        assertEquals("content://photo/1", state.vehicles.get(0).getImagemUrl());
        assertNull(state.vehicles.get(0).getConsumo()); assertNull(state.vehicles.get(0).getPotencia());
    }
    @Test public void monthlyPerformanceCountsOpeningsAndCompletedContactsPerDealer() {
        long month = 1_000L;
        assertTrue(state.recordView("user", vehicle.getId(), month));
        assertTrue(state.recordView("user", vehicle.getId(), month + 1));
        assertFalse(state.recordView("dealer-a", vehicle.getId(), month + 2));
        assertFalse(state.recordView("user", "missing", month + 3));
        state.addInterest("user", "dealer-a", vehicle);
        Interest interest = state.interests.get(0); interest.createdAt = month + 4;
        state.updateStatus("dealer-a", interest.id, "Finalizado");
        DealerPerformance result = state.performance("dealer-a", month);
        assertEquals(2, result.views); assertEquals(1, result.contacts); assertEquals(100, result.conversionPercent);
        DealerPerformance other = state.performance("dealer-b", month);
        assertEquals(0, other.views); assertEquals(0, other.contacts); assertEquals(0, other.conversionPercent);
    }
    @Test public void oldEventsAreExcludedAndDeletingVehiclePreservesViewHistory() {
        state.recordView("user", vehicle.getId(), 999);
        state.recordView("user", vehicle.getId(), 1001);
        state.deleteVehicle("dealer-a", vehicle.getId()); state = restart();
        assertEquals(1, state.performance("dealer-a", 1000).views);
    }
    @Test public void performanceMilestonesAreStableAtBoundaries() {
        assertEquals(10, DealerPerformance.nextMilestone(0));
        assertEquals(10, DealerPerformance.nextMilestone(10));
        assertEquals(50, DealerPerformance.nextMilestone(11));
        assertEquals(50, DealerPerformance.nextMilestone(50));
        assertEquals(1000, DealerPerformance.nextMilestone(847));
        assertEquals(10000, DealerPerformance.nextMilestone(5001));
    }
    @Test public void dealerNotificationPreferencesDefaultEnabledAndRemainIsolated() {
        assertTrue(state.account("dealer-a").notificationsEnabled());
        assertTrue(state.account("dealer-a").newInterestsEnabled());
        state.account("dealer-a").notificationsEnabled = false;
        state.account("dealer-a").newInterestsEnabled = false;
        state = restart();
        assertFalse(state.account("dealer-a").notificationsEnabled());
        assertFalse(state.account("dealer-a").newInterestsEnabled());
        assertTrue(state.account("dealer-b").notificationsEnabled());
        assertTrue(state.account("dealer-b").newInterestsEnabled());
    }
}

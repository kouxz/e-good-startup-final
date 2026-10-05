package com.projeto.egoodapp.data.local;

import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.model.DealerPerformance;
import com.projeto.egoodapp.data.model.DealerRatingSummary;
import com.projeto.egoodapp.data.model.Interest;

import com.google.gson.Gson;
import com.projeto.egoodapp.data.model.Vehicle;
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
        assertTrue(state.updateInterest("dealer-a", lead.id,
                InterestWorkflow.STATUS_IN_CONTACT, null));
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
        assertFalse(state.updateInterest("dealer-b", lead.id,
                InterestWorkflow.STATUS_FINISHED, InterestWorkflow.OUTCOME_SALE));
        assertTrue(state.updateInterest("dealer-a", lead.id,
                InterestWorkflow.STATUS_FINISHED, InterestWorkflow.OUTCOME_SALE));
        assertFalse(state.updateInterest("dealer-a", lead.id, "Inválido", null));
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
    @Test public void monthlyPerformanceCountsOpeningsSalesAndContactsPerDealer() {
        long month = 1_000L;
        long nextMonth = 2_000L;
        assertTrue(state.recordView("user", vehicle.getId(), month));
        assertTrue(state.recordView("user", vehicle.getId(), month + 1));
        assertFalse(state.recordView("dealer-a", vehicle.getId(), month + 2));
        assertFalse(state.recordView("user", "missing", month + 3));
        state.addInterest("user", "dealer-a", vehicle);
        Interest interest = state.interests.get(0); interest.createdAt = month + 4;
        state.updateInterest("dealer-a", interest.id,
                InterestWorkflow.STATUS_FINISHED, InterestWorkflow.OUTCOME_SALE);
        DealerPerformance result = state.performance("dealer-a", month, nextMonth);
        assertEquals(2, result.views); assertEquals(1, result.contacts);
        assertEquals(1, result.sales); assertEquals(100, result.conversionPercent);
        DealerPerformance other = state.performance("dealer-b", month, nextMonth);
        assertEquals(0, other.views); assertEquals(0, other.contacts); assertEquals(0, other.conversionPercent);
    }
    @Test public void oldEventsAreExcludedAndDeletingVehiclePreservesViewHistory() {
        state.recordView("user", vehicle.getId(), 999);
        state.recordView("user", vehicle.getId(), 1001);
        state.deleteVehicle("dealer-a", vehicle.getId()); state = restart();
        assertEquals(1, state.performance("dealer-a", 1000, 2000).views);
    }
    @Test public void finishingRequiresOutcomeAndNoSaleDoesNotConvert() {
        state.addInterest("user", "dealer-a", vehicle);
        Interest lead = state.interests.get(0);
        lead.createdAt = 1_100L;

        assertFalse(state.updateInterest("dealer-a", lead.id,
                InterestWorkflow.STATUS_FINISHED, null));
        assertEquals(InterestWorkflow.STATUS_NEW, lead.status);
        assertTrue(state.updateInterest("dealer-a", lead.id,
                InterestWorkflow.STATUS_FINISHED, InterestWorkflow.OUTCOME_NO_SALE));
        DealerPerformance performance = state.performance("dealer-a", 1_000L, 2_000L);
        assertEquals(1, performance.contacts);
        assertEquals(0, performance.sales);
        assertEquals(0, performance.conversionPercent);
    }
    @Test public void reopeningFinishedContactClearsItsOutcomeAndConversion() {
        state.addInterest("user", "dealer-a", vehicle);
        Interest lead = state.interests.get(0);
        lead.createdAt = 1_100L;
        assertTrue(state.updateInterest("dealer-a", lead.id,
                InterestWorkflow.STATUS_FINISHED, InterestWorkflow.OUTCOME_SALE));
        assertEquals(1, state.performance("dealer-a", 1_000L, 2_000L).sales);

        assertTrue(state.updateInterest("dealer-a", lead.id,
                InterestWorkflow.STATUS_IN_CONTACT, null));
        assertEquals("", lead.outcome);
        assertEquals(0, state.performance("dealer-a", 1_000L, 2_000L).sales);
    }
    @Test public void finishedOutcomeCanBeCorrectedAndInvalidOutcomeIsRejected() {
        state.addInterest("user", "dealer-a", vehicle);
        Interest lead = state.interests.get(0);
        lead.createdAt = 1_100L;
        assertFalse(state.updateInterest("dealer-a", lead.id,
                InterestWorkflow.STATUS_FINISHED, "INVALID"));
        assertEquals(InterestWorkflow.STATUS_NEW, lead.status);

        assertTrue(state.updateInterest("dealer-a", lead.id,
                InterestWorkflow.STATUS_FINISHED, InterestWorkflow.OUTCOME_SALE));
        assertTrue(state.updateInterest("dealer-a", lead.id,
                InterestWorkflow.STATUS_FINISHED, InterestWorkflow.OUTCOME_NO_SALE));
        assertEquals(InterestWorkflow.OUTCOME_NO_SALE, lead.outcome);
        assertEquals(0, state.performance("dealer-a", 1_000L, 2_000L).sales);
    }
    @Test public void conversionPercentageRoundsToNearestInteger() {
        state.saveAccount(profile("user-2", "pessoa"));
        state.saveAccount(profile("user-3", "pessoa"));
        state.addInterest("user", "dealer-a", vehicle);
        state.addInterest("user-2", "dealer-a", vehicle);
        state.addInterest("user-3", "dealer-a", vehicle);
        for (Interest lead : state.interests) lead.createdAt = 1_100L;
        Interest sale = state.interests.get(0);
        assertTrue(state.updateInterest("dealer-a", sale.id,
                InterestWorkflow.STATUS_FINISHED, InterestWorkflow.OUTCOME_SALE));

        DealerPerformance performance = state.performance("dealer-a", 1_000L, 2_000L);
        assertEquals(3, performance.contacts);
        assertEquals(1, performance.sales);
        assertEquals(33, performance.conversionPercent);
    }
    @Test public void oldFinishedContactMigratesToSaleAndPeriodEndIsExclusive() {
        state.addInterest("user", "dealer-a", vehicle);
        Interest lead = state.interests.get(0);
        lead.status = InterestWorkflow.STATUS_FINISHED;
        lead.outcome = null;
        lead.createdAt = 2_000L;

        assertTrue(state.migrateLegacyInterests());
        assertEquals(InterestWorkflow.OUTCOME_SALE, lead.outcome);
        assertEquals(0, state.performance("dealer-a", 1_000L, 2_000L).contacts);
        assertEquals(1, state.performance("dealer-a", 2_000L, 3_000L).sales);
        assertFalse(state.migrateLegacyInterests());
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

    @Test public void dealerRatingsCreateUpdateAverageAndRemovePerUser() {
        state.saveAccount(profile("second-user", "pessoa"));
        state.rateDealer("user", "local:dealer-a", 5, 100);
        state.rateDealer("second-user", "local:dealer-a", 3, 101);

        DealerRatingSummary first = state.dealerRating("local:dealer-a", "user");
        assertEquals(4.0, first.average, 0.001);
        assertEquals(2, first.count);
        assertEquals(Integer.valueOf(5), first.userScore);

        state.rateDealer("user", "local:dealer-a", 4, 102);
        DealerRatingSummary updated = state.dealerRating("local:dealer-a", "user");
        assertEquals(3.5, updated.average, 0.001);
        assertEquals(2, updated.count);
        assertEquals(Integer.valueOf(4), updated.userScore);

        assertTrue(state.removeDealerRating("user", "local:dealer-a"));
        assertFalse(state.removeDealerRating("user", "local:dealer-a"));
        DealerRatingSummary remaining = state.dealerRating("local:dealer-a", "user");
        assertEquals(3.0, remaining.average, 0.001);
        assertEquals(1, remaining.count);
        assertNull(remaining.userScore);
    }

    @Test public void dealerRatingsRemainIsolatedAndSurviveSerialization() {
        state.rateDealer("user", "local:dealer-a", 5, 100);
        state.rateDealer("user", "osm:node:42", 2, 101);
        state = restart();

        assertEquals(5.0, state.dealerRating("local:dealer-a", "user").average, 0.001);
        assertEquals(2.0, state.dealerRating("osm:node:42", "user").average, 0.001);
        assertEquals(0, state.dealerRating("local:dealer-b", "user").count);

        state.dealerRatings = null;
        assertEquals(0, state.dealerRating("local:dealer-a", "user").count);
        state.rateDealer("user", "local:dealer-a", 4, 102);
        assertEquals(1, state.dealerRating("local:dealer-a", "user").count);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dealerAccountCannotRateAnotherDealer() {
        state.rateDealer("dealer-a", "local:dealer-b", 5, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ratingOutsideOneToFiveIsRejected() {
        state.rateDealer("user", "local:dealer-a", 0, 100);
    }

    @Test public void deletingPersonRemovesTheirPrivateHistoryAndPreservesOtherAccounts() {
        state.saveAccount(profile("second-user", "pessoa"));
        state.addInterest("user", "dealer-a", vehicle);
        state.addInterest("second-user", "dealer-a", vehicle);
        state.recordView("user", vehicle.getId(), 100);
        state.recordView("second-user", vehicle.getId(), 101);
        state.rateDealer("user", "local:dealer-a", 5, 100);
        state.rateDealer("user", "osm:node:42", 4, 101);
        state.rateDealer("second-user", "local:dealer-a", 3, 102);

        assertTrue(state.deleteAccount("user").isEmpty());
        state = restart();

        assertNull(state.account("user"));
        assertNotNull(state.account("second-user"));
        assertNotNull(state.account("dealer-a"));
        assertEquals(1, state.interests.size());
        assertEquals("second-user", state.interests.get(0).userId);
        assertEquals(1, state.vehicleViews.size());
        assertEquals("second-user", state.vehicleViews.get(0).userId);
        assertEquals(1, state.dealerRating("local:dealer-a", "second-user").count);
        assertEquals(0, state.dealerRating("osm:node:42", "user").count);
        assertEquals(1, state.dealerVehicles("dealer-a").size());
    }

    @Test public void deletingDealerRemovesOwnedContentAndKeepsUnrelatedData() {
        state.saveAccount(profile("second-user", "pessoa"));
        Vehicle otherVehicle = new Vehicle();
        otherVehicle.setId("dealer-b-car");
        otherVehicle.setConcessionariaId("dealer-b");
        otherVehicle.setImagemUrl("file:///other.img");
        state.vehicles.add(otherVehicle);
        state.addInterest("user", "dealer-a", vehicle);
        state.recordView("user", vehicle.getId(), 100);
        state.rateDealer("user", "local:dealer-a", 5, 100);
        state.rateDealer("second-user", "local:dealer-a", 4, 101);
        state.rateDealer("user", "local:dealer-b", 3, 102);
        state.rateDealer("user", "osm:node:42", 2, 103);

        java.util.List<String> photos = state.deleteAccount("dealer-a");
        state = restart();

        assertEquals(java.util.Collections.singletonList("file:///foto.img"), photos);
        assertNull(state.account("dealer-a"));
        assertNotNull(state.account("dealer-b"));
        assertTrue(state.dealerVehicles("dealer-a").isEmpty());
        assertEquals(1, state.dealerVehicles("dealer-b").size());
        assertTrue(state.interests.isEmpty());
        assertTrue(state.vehicleViews.isEmpty());
        assertEquals(0, state.dealerRating("local:dealer-a", "user").count);
        assertEquals(1, state.dealerRating("local:dealer-b", "user").count);
        assertEquals(1, state.dealerRating("osm:node:42", "user").count);
    }
}

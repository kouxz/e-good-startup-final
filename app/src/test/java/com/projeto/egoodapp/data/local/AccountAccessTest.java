package com.projeto.egoodapp.data.local;

import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.model.Interest;

import com.google.gson.Gson;
import com.projeto.egoodapp.data.model.Vehicle;
import org.junit.Test;
import static org.junit.Assert.*;

public class AccountAccessTest {
    private AccountProfile profile(String uid, String type) {
        AccountProfile profile = new AccountProfile();
        profile.uid = uid; profile.type = type; profile.name = "Conta existente";
        profile.email = "conta@example.com"; profile.phone = "11999999999";
        return profile;
    }

    @Test public void personIsAcceptedOnlyInPersonTabWithoutChangingProfile() {
        AccountProfile person = profile("person-uid", "pessoa");
        String before = new Gson().toJson(person);
        assertEquals(AccountAccess.Decision.ALLOW, AccountAccess.evaluate(person, "pessoa"));
        assertEquals(AccountAccess.Decision.REJECT, AccountAccess.evaluate(person, "concessionaria"));
        assertEquals(before, new Gson().toJson(person));
    }

    @Test public void dealershipIsAcceptedOnlyInDealerTabWithoutChangingProfile() {
        AccountProfile dealer = profile("dealer-uid", "concessionaria");
        String before = new Gson().toJson(dealer);
        assertEquals(AccountAccess.Decision.ALLOW, AccountAccess.evaluate(dealer, "concessionaria"));
        assertEquals(AccountAccess.Decision.REJECT, AccountAccess.evaluate(dealer, "pessoa"));
        assertEquals(before, new Gson().toJson(dealer));
    }

    @Test public void unknownAccountRequiresConfirmationWithoutCreatingProfile() {
        LocalState state = new LocalState();
        assertEquals(AccountAccess.Decision.CONFIRM_TYPE, AccountAccess.evaluate(state.account("old"), "pessoa"));
        assertEquals(AccountAccess.Decision.CONFIRM_TYPE, AccountAccess.evaluate(state.account("old"), "concessionaria"));
        assertTrue(state.accounts.isEmpty());
        assertEquals(AccountAccess.Decision.CONFIRM_TYPE, AccountAccess.evaluate(profile("old", null), "pessoa"));
        assertEquals(AccountAccess.Decision.CONFIRM_TYPE, AccountAccess.evaluate(profile("old", "legacy"), "pessoa"));
    }

    @Test public void confirmedTypeSurvivesRestartAndCannotBeOverwrittenByTheOtherTab() {
        LocalState state = new LocalState();
        state.confirmAccountType("old", "pessoa", "Nome Google", "google@example.com");
        state = new Gson().fromJson(new Gson().toJson(state), LocalState.class);
        AccountProfile result = state.confirmAccountType("old", "concessionaria", "Outro nome", "outro@example.com");
        assertEquals("pessoa", result.type);
        assertEquals("Nome Google", result.name);
        assertEquals("google@example.com", result.email);
        assertEquals(1, state.accounts.size());
        assertEquals(AccountAccess.Decision.REJECT, AccountAccess.evaluate(result, "concessionaria"));
    }

    @Test public void identifyingLegacyProfilePreservesItsContactAndCompanyData() {
        LocalState state = new LocalState();
        AccountProfile old = profile("old", null);
        old.cnpj = "12345678000199"; old.address = "Rua existente"; old.city = "Sorocaba"; old.state = "SP";
        state.saveAccount(old);
        AccountProfile result = state.confirmAccountType("old", "concessionaria", "Google name", "google@example.com");
        assertEquals("Conta existente", result.name);
        assertEquals("11999999999", result.phone);
        assertEquals("12345678000199", result.cnpj);
        assertEquals("Rua existente", result.address);
        assertEquals(AccountAccess.Decision.ALLOW, AccountAccess.evaluate(result, "concessionaria"));
    }

    @Test public void accessUsesUidAndPreservesVehiclesAndInterestsEvenForMatchingEmails() {
        LocalState state = new LocalState();
        state.saveAccount(profile("person", "pessoa"));
        state.saveAccount(profile("dealer", "concessionaria"));
        Vehicle vehicle = new Vehicle(); vehicle.setId("car"); vehicle.setConcessionariaId("dealer");
        state.vehicles.add(vehicle);
        Interest lead = new Interest(); lead.id = "lead"; lead.dealerId = "dealer"; lead.userId = "person";
        state.interests.add(lead);
        String before = new Gson().toJson(state);
        assertEquals(AccountAccess.Decision.REJECT, AccountAccess.evaluate(state.account("person"), "concessionaria"));
        assertEquals(AccountAccess.Decision.ALLOW, AccountAccess.evaluate(state.account("dealer"), "concessionaria"));
        assertEquals(before, new Gson().toJson(state));
    }

    @Test(expected = IllegalArgumentException.class) public void invalidConfirmationIsRejected() {
        new LocalState().confirmAccountType("uid", "unknown", "Nome", "email@example.com");
    }
}

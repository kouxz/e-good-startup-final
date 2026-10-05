package com.projeto.egoodapp.security;

import com.google.gson.Gson;
import com.projeto.egoodapp.data.model.AccountProfile;
import org.junit.Test;
import static org.junit.Assert.*;

public class PrivacyAndInputTest {
    @Test public void legacyProfilesNeverImplyConsent() {
        AccountProfile profile = new Gson().fromJson("{\"uid\":\"test\",\"name\":\"Pessoa\"}", AccountProfile.class);
        assertFalse(PrivacyPolicy.acknowledged(profile)); assertFalse(PrivacyPolicy.locationAllowed(profile));
        PrivacyPolicy.acknowledge(profile, 10); assertTrue(PrivacyPolicy.acknowledged(profile));
        assertFalse(PrivacyPolicy.locationAllowed(profile));
    }
    @Test public void grantRevokeAndVersionChangesSurviveSerialization() {
        AccountProfile profile = new AccountProfile(); PrivacyPolicy.location(profile, true, 10);
        Gson gson = new Gson(); profile = gson.fromJson(gson.toJson(profile), AccountProfile.class);
        assertTrue(PrivacyPolicy.locationAllowed(profile));
        profile.locationConsentVersion = "old"; assertFalse(PrivacyPolicy.locationAllowed(profile));
        PrivacyPolicy.location(profile, false, 20);
        assertTrue(PrivacyPolicy.locationDecided(profile)); assertFalse(PrivacyPolicy.locationAllowed(profile));
        assertEquals(Long.valueOf(20), profile.locationConsentUpdatedAt);
    }
    @Test public void fictionalEmailsAndCnpjsArePreservedAndControlsAreRemoved() {
        assertEquals("teste@exemplo.invalid", InputRules.clean("teste@exemplo.invalid", InputRules.EMAIL));
        assertEquals("11111111111111", InputRules.clean("11111111111111", InputRules.CNPJ));
        assertEquals("Nome", InputRules.clean(" No\u0000me\u202e ", InputRules.NAME));
        assertEquals("<script>alert(1)</script>", InputRules.clean("<script>alert(1)</script>", InputRules.DESCRIPTION));
    }
    @Test(expected = IllegalArgumentException.class) public void excessiveLengthIsRejectedWithoutTruncatingStoredData() {
        InputRules.clean("1".repeat(15), InputRules.CNPJ);
    }
}

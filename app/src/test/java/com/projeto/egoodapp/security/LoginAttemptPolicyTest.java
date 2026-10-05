package com.projeto.egoodapp.security;

import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.*;

public class LoginAttemptPolicyTest {
    @Test public void fifthCredentialFailureBlocksForExactlyOneMinute() {
        LoginAttemptPolicy policy = new LoginAttemptPolicy();
        for (int i = 0; i < 4; i++) assertFalse(policy.failure(1000, true, false));
        assertTrue(policy.failure(1000, true, false));
        assertEquals(60000, policy.remaining(1000));
        assertFalse(policy.failure(2000, true, false));
        assertEquals(1, policy.remaining(60999));
        assertEquals(0, policy.remaining(61000));
        assertFalse(policy.failure(61000, true, false));
        assertEquals(1, policy.failures);
    }
    @Test public void networkAndCancellationDoNotCountAndSuccessResets() {
        LoginAttemptPolicy policy = new LoginAttemptPolicy();
        for (int i = 0; i < 10; i++) policy.failure(1000, false, false);
        assertEquals(0, policy.failures);
        policy.failure(1000, true, false); policy.success();
        assertEquals(0, policy.failures); assertEquals(0, policy.remaining(1000));
    }
    @Test public void serverCooldownPersistsAcrossRecreationAndClockRollbackIsBounded() {
        LoginAttemptPolicy policy = new LoginAttemptPolicy();
        assertTrue(policy.failure(10000, false, true));
        Gson gson = new Gson(); policy = gson.fromJson(gson.toJson(policy), LoginAttemptPolicy.class);
        assertEquals(299000, policy.remaining(11000));
        assertEquals(300000, policy.remaining(1));
        assertEquals(0, policy.remaining(300001));
    }
}

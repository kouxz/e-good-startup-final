package com.projeto.egoodapp.data.local;

import com.projeto.egoodapp.data.model.SecurityAuditEvent;

import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.*;

public class SecurityAuditTest {
    @Test public void retainsOnlyNewestTwoHundredEventsAndAtMostNinetyDays() {
        LocalState state = new LocalState(); long now = LocalState.AUDIT_RETENTION_MS + 100;
        state.audit(SecurityAuditEvent.Type.LOGIN, SecurityAuditEvent.Result.SUCCESS, "old", 1);
        for (int i = 0; i < 201; i++) state.audit(SecurityAuditEvent.Type.LOGIN, SecurityAuditEvent.Result.FAILURE, null, now + i);
        assertEquals(200, state.securityAudit.size());
        assertEquals(now + 1, state.securityAudit.get(0).createdAt);
        state.pruneAudit(now + 200 + LocalState.AUDIT_RETENTION_MS);
        assertTrue(state.securityAudit.isEmpty());
    }
    @Test public void deletingAccountRemovesOnlyItsAuditAndSession() {
        LocalState state = new LocalState();
        state.audit(SecurityAuditEvent.Type.LOGOUT, SecurityAuditEvent.Result.SUCCESS, "person", 10);
        state.audit(SecurityAuditEvent.Type.LOGIN, SecurityAuditEvent.Result.SUCCESS, "other", 10);
        state.audit(SecurityAuditEvent.Type.LOGIN_COOLDOWN, SecurityAuditEvent.Result.SUCCESS, null, 10);
        state.sessionDeadline = new com.projeto.egoodapp.security.SessionDeadline();
        state.sessionDeadline.userId = "person";
        state.deleteAccount("person"); assertNull(state.sessionDeadline);
        assertEquals(2, state.securityAudit.size()); assertEquals("other", state.securityAudit.get(0).userId);
    }
    @Test public void eventSchemaHasNoFreeFormInputAndSurvivesSerialization() {
        LocalState state = new LocalState(); state.audit(SecurityAuditEvent.Type.PRIVACY_ACKNOWLEDGED,
                SecurityAuditEvent.Result.SUCCESS, "uid", 10);
        Gson gson = new Gson(); String json = gson.toJson(state.securityAudit.get(0));
        assertEquals(4, com.google.gson.JsonParser.parseString(json).getAsJsonObject().size());
        LocalState restored = gson.fromJson(gson.toJson(state), LocalState.class);
        assertEquals(SecurityAuditEvent.Type.PRIVACY_ACKNOWLEDGED, restored.securityAudit.get(0).type);
    }
}

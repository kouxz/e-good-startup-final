package com.projeto.egoodapp.data.local;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.projeto.egoodapp.data.model.SecurityAuditEvent;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

/** The fixture was serialized by the pre-refactor compiled classes; all data is fictitious. */
public class PackageRefactorCompatibilityTest {
    @Test public void preRefactorSnapshotReadsWithoutLosingFieldsOrChangingJsonSchema() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/compatibility/local-state-before-package-refactor.json")) {
            assertNotNull(stream);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            LocalState state = gson.fromJson(json, LocalState.class);
            assertEquals("Empresa de teste", state.account("fixture-dealer").name);
            assertFalse(state.account("fixture-dealer").notificationsEnabled());
            assertEquals("Pessoa de teste", state.account("fixture-person").name);
            assertEquals("fixture-vehicle", state.vehicles.get(0).getId());
            assertEquals("fixture-dealer", state.vehicles.get(0).getConcessionariaId());
            assertEquals("BYD Dolphin Mini", state.vehicles.get(0).getNome());
            assertEquals(12.9, state.vehicles.get(0).getConsumo(), 0);
            assertEquals("file:///private/vehicle_photos/fixture.jpg", state.vehicles.get(0).getImagemUrl());
            assertEquals("SALE", state.interests.get(0).outcome);
            assertEquals("Finalizado", state.interests.get(0).status);
            assertEquals("fixture-person", state.vehicleViews.get(0).userId);
            assertEquals(5, state.dealerRating("local:fixture-dealer", "fixture-person").average, 0);
            assertEquals(1, state.dealerRatings.size());
            assertEquals(SecurityAuditEvent.Type.LOGIN, state.securityAudit.get(0).type);
            assertEquals(SecurityAuditEvent.Result.SUCCESS, state.securityAudit.get(0).result);
            assertEquals("fixture-person", state.sessionDeadline.userId);
            assertTrue(state.legacyImported); assertTrue(state.legacyAssigned);
            assertEquals(JsonParser.parseString(json), JsonParser.parseString(gson.toJson(state)));
        }
    }
}

package com.projeto.egoodapp.data.nearby;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NearbyLocationPolicyTest {
    private static final long NOW = 1_800_000_000_000L;

    @Test
    public void acceptsValidRecentCachedLocation() {
        assertTrue(NearbyLocationPolicy.canUseCached(
                -23.5015, -47.4581, NOW - 60_000L, NOW));
    }

    @Test
    public void rejectsOldMissingAndInvalidCachedLocations() {
        assertFalse(NearbyLocationPolicy.canUseCached(
                -23.5015, -47.4581,
                NOW - NearbyLocationPolicy.MAX_CACHED_LOCATION_AGE_MS - 1L, NOW));
        assertFalse(NearbyLocationPolicy.canUseCached(-23.5015, -47.4581, 0L, NOW));
        assertFalse(NearbyLocationPolicy.canUseCached(Double.NaN, -47.4581, NOW, NOW));
        assertFalse(NearbyLocationPolicy.canUseCached(0d, 0d, NOW, NOW));
    }

    @Test
    public void validatesCoordinateLimits() {
        assertTrue(NearbyLocationPolicy.hasValidCoordinates(-90d, -180d));
        assertTrue(NearbyLocationPolicy.hasValidCoordinates(90d, 180d));
        assertFalse(NearbyLocationPolicy.hasValidCoordinates(-90.01d, 0d));
        assertFalse(NearbyLocationPolicy.hasValidCoordinates(0d, 180.01d));
        assertFalse(NearbyLocationPolicy.hasValidCoordinates(
                Double.POSITIVE_INFINITY, 10d));
    }

    @Test
    public void requestGateAcceptsOnlyTheFirstCompletion() {
        NearbyLocationPolicy.RequestGate gate = new NearbyLocationPolicy.RequestGate();
        int request = gate.begin();

        assertTrue(gate.isActive(request));
        assertTrue(gate.complete(request));
        assertFalse(gate.complete(request));
        assertFalse(gate.isActive(request));
    }

    @Test
    public void requestGateIgnoresLateResultsAndCancelledRequests() {
        NearbyLocationPolicy.RequestGate gate = new NearbyLocationPolicy.RequestGate();
        int oldRequest = gate.begin();
        int currentRequest = gate.begin();

        assertFalse(gate.complete(oldRequest));
        assertTrue(gate.complete(currentRequest));

        int cancelledRequest = gate.begin();
        gate.cancel();
        assertFalse(gate.complete(cancelledRequest));
    }
}

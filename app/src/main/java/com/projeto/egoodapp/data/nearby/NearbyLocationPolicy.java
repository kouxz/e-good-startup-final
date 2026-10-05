package com.projeto.egoodapp.data.nearby;

public final class NearbyLocationPolicy {
    static final long MAX_CACHED_LOCATION_AGE_MS = 10 * 60 * 1000L;

    private NearbyLocationPolicy() {
    }

    public static boolean hasValidCoordinates(double latitude, double longitude) {
        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90d
                && latitude <= 90d
                && longitude >= -180d
                && longitude <= 180d
                && (latitude != 0d || longitude != 0d);
    }

    static boolean isFresh(long locationTimeMillis, long nowMillis) {
        if (locationTimeMillis <= 0L) return false;
        long age = Math.max(0L, nowMillis - locationTimeMillis);
        return age <= MAX_CACHED_LOCATION_AGE_MS;
    }

    public static boolean canUseCached(double latitude, double longitude,
            long locationTimeMillis, long nowMillis) {
        return hasValidCoordinates(latitude, longitude)
                && isFresh(locationTimeMillis, nowMillis);
    }

    public static final class RequestGate {
        private int generation;
        private boolean pending;

        public int begin() {
            pending = true;
            return ++generation;
        }

        public boolean isActive(int requestGeneration) {
            return pending && requestGeneration == generation;
        }

        public boolean complete(int requestGeneration) {
            if (!isActive(requestGeneration)) return false;
            pending = false;
            return true;
        }

        public void cancel() {
            pending = false;
            generation++;
        }
    }
}

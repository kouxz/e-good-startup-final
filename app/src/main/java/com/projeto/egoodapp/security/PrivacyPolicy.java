package com.projeto.egoodapp.security;

import com.projeto.egoodapp.data.model.AccountProfile;

public final class PrivacyPolicy {
    public static final String VERSION = "2026-10-05";
    public static final String LOCATION_VERSION = "2026-10-05";
    private PrivacyPolicy() {}
    public static boolean acknowledged(AccountProfile profile) {
        return profile != null && VERSION.equals(profile.privacyVersion) && profile.privacyAcknowledgedAt != null;
    }
    public static boolean locationDecided(AccountProfile profile) {
        return profile != null && LOCATION_VERSION.equals(profile.locationConsentVersion)
                && profile.locationAllowed != null;
    }
    public static boolean locationAllowed(AccountProfile profile) {
        return locationDecided(profile) && Boolean.TRUE.equals(profile.locationAllowed);
    }
    public static void acknowledge(AccountProfile profile, long now) {
        profile.privacyVersion = VERSION; profile.privacyAcknowledgedAt = now;
    }
    public static void location(AccountProfile profile, boolean allowed, long now) {
        profile.locationAllowed = allowed; profile.locationConsentVersion = LOCATION_VERSION;
        profile.locationConsentUpdatedAt = now;
    }
}

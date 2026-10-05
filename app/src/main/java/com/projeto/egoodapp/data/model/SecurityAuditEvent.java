package com.projeto.egoodapp.data.model;

/** No free-form detail: credentials, inputs and exceptions cannot enter the audit schema. */
public final class SecurityAuditEvent {
    public enum Type { LOGIN, LOGIN_COOLDOWN, LOGOUT, SESSION_EXPIRED, PASSWORD_RESET,
        PRIVACY_ACKNOWLEDGED, LOCATION_PERMISSION, ACCOUNT_DELETION }
    public enum Result { SUCCESS, FAILURE, GRANTED, DENIED, REVOKED }
    public Type type;
    public Result result;
    public String userId;
    public long createdAt;

    public SecurityAuditEvent(Type type, Result result, String userId, long createdAt) {
        this.type = type; this.result = result; this.userId = userId; this.createdAt = createdAt;
    }
}

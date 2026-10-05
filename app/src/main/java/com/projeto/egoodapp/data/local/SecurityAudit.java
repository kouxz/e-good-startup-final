package com.projeto.egoodapp.data.local;

import com.projeto.egoodapp.data.model.SecurityAuditEvent;

import android.content.Context;

/** A failed diagnostic write must never prevent logout, deletion or another security action. */
public final class SecurityAudit {
    private SecurityAudit() {}
    public static void record(Context context, SecurityAuditEvent.Type type,
            SecurityAuditEvent.Result result, String uid) {
        try { LocalRepository.get(context).audit(type, result, uid); }
        catch (RuntimeException ignored) { /* Do not reveal storage errors or retain sensitive detail. */ }
    }
}

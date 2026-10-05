package com.projeto.egoodapp.security;

import android.content.Context;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.SecurityAudit;
import com.projeto.egoodapp.data.model.SecurityAuditEvent;
import com.projeto.egoodapp.views.auth.AuthErrorMessages;

public final class LoginAttemptLimiter {
    private final Context context;
    private final LocalRepository repository;
    public LoginAttemptLimiter(Context context) {
        this.context = context.getApplicationContext(); repository = LocalRepository.get(context);
    }
    public long remaining() {
        LoginAttemptPolicy policy = repository.loginAttempts();
        long previousBlock = policy.blockedUntil, previousObserved = policy.observedAt;
        long remaining = policy.remaining(System.currentTimeMillis());
        if (policy.blockedUntil != previousBlock || previousObserved > policy.observedAt) repository.saveLoginAttempts(policy);
        return remaining;
    }
    public void failure(Exception failure) {
        LoginAttemptPolicy policy = repository.loginAttempts();
        boolean blocked = policy.failure(System.currentTimeMillis(), AuthErrorMessages.invalidCredentials(failure),
                AuthErrorMessages.tooManyRequests(failure));
        repository.saveLoginAttempts(policy);
        if (AuthErrorMessages.invalidCredentials(failure)) SecurityAudit.record(context,
                SecurityAuditEvent.Type.LOGIN, SecurityAuditEvent.Result.FAILURE, null);
        if (blocked) SecurityAudit.record(context, SecurityAuditEvent.Type.LOGIN_COOLDOWN,
                SecurityAuditEvent.Result.SUCCESS, null);
    }
    public void success() {
        LoginAttemptPolicy policy = repository.loginAttempts(); policy.success(); repository.saveLoginAttempts(policy);
    }
}

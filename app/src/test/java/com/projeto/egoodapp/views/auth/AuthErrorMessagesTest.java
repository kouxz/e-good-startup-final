package com.projeto.egoodapp.views.auth;

import com.projeto.egoodapp.views.common.feedback.ErrorMessages;

import org.junit.Test;
import static org.junit.Assert.*;

public class AuthErrorMessagesTest {
    @Test public void registrationDoesNotExposeFirebaseExceptionMessage() {
        assertEquals("Não foi possível cadastrar esse e-mail. Tente entrar ou recuperar a senha.",
                AuthErrorMessages.registrationCode("ERROR_EMAIL_ALREADY_IN_USE"));
    }

    @Test public void unknownRegistrationFailureUsesGenericMessage() {
        assertEquals("Não foi possível concluir o cadastro. Tente novamente.",
                AuthErrorMessages.registration(new IllegalStateException("internal path")));
        assertEquals("Não foi possível concluir o cadastro. Tente novamente.",
                AuthErrorMessages.registration(null));
    }

    @Test public void loginAndRecoveryNeverRenderExternalDetails() {
        Exception failure = new IllegalStateException("/private/data token=secret HTTP 500 database");
        assertEquals("Não foi possível concluir a solicitação. Tente novamente.", AuthErrorMessages.login(failure, false));
        assertEquals("Não foi possível concluir a solicitação. Tente novamente.", AuthErrorMessages.loginCode("INTERNAL_DATABASE_SECRET", false));
        assertEquals(AuthErrorMessages.loginCode("ERROR_WRONG_PASSWORD", false),
                AuthErrorMessages.loginCode("ERROR_USER_NOT_FOUND", false));
        assertEquals("Não foi possível concluir a operação. Tente novamente.", ErrorMessages.safe(failure));
        assertEquals("A foto deve ter no máximo 10 MB.", ErrorMessages.safe(new java.io.IOException("A foto deve ter no máximo 10 MB.")));
        assertFalse(AuthErrorMessages.invalidCredentials(null));
    }
}

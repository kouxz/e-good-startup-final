package com.projeto.egoodapp.security;

import org.junit.Test;
import static org.junit.Assert.*;

public class PasswordPolicyTest {
    @Test public void checksEveryRequirementWithoutChangingThePassword() {
        assertTrue(PasswordPolicy.valid("Teste123!"));
        assertTrue(PasswordPolicy.valid(" Abc123!"));
        for (String weak : new String[]{"", "abc", "teste123!", "TESTE123!", "Testeabc!", "Teste1234", "Abc123!"})
            assertFalse(weak, PasswordPolicy.valid(weak));
    }
    @Test public void lengthBoundariesAndFirebaseSymbolsMatch() {
        assertTrue(PasswordPolicy.valid("Aa1!" + "x".repeat(26)));
        assertFalse(PasswordPolicy.valid("Aa1!" + "x".repeat(27)));
        assertTrue(PasswordPolicy.valid("Senha123_"));
        assertFalse(PasswordPolicy.valid("Senha123-"));
        assertFalse(PasswordPolicy.valid(null));
    }
}

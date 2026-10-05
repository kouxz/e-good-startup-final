package com.projeto.egoodapp.views.profile;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class UserSupportTest {
    @Test
    public void developerDialogContainsTheCompleteTeam() {
        assertArrayEquals(new String[] {
                "Fernanda Falcão Kedouk Simões",
                "Giovanni Pereira Valente",
                "Kauã Garcia Francisco",
                "Maria Eduarda Souza Santos",
                "Nicole Kerne"
        }, UserSupport.developersText().split("\\n"));
    }
}

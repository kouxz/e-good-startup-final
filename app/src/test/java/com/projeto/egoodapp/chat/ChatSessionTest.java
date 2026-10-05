package com.projeto.egoodapp.chat;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChatSessionTest {
    @After public void cleanup() { ChatSession.clear(); }
    @Test public void reopensWithSameHistoryStateAndDraft() {
        ChatSession first = ChatSession.forUser("a"); first.engine.send("solar"); first.draft("5,5");
        ChatSession reopened = ChatSession.forUser("a"); assertSame(first, reopened);
        assertEquals(ChatEngine.Step.SOLAR_POWER, reopened.engine.step()); assertEquals("5,5", reopened.draft());
        ChatSession.syncUser("a"); assertTrue(first.active());
    }
    @Test public void accountChangeWipesOldReferencesAndDraft() {
        ChatSession first = ChatSession.forUser("a"); first.engine.send("meu carro"); first.draft("segredo");
        ChatSession next = ChatSession.forUser("b");
        assertFalse(first.active()); assertTrue(first.engine.messages().isEmpty()); assertEquals("", first.draft());
        first.draft("novo"); assertEquals("", first.draft());
        assertEquals(1, next.engine.messages().size()); assertEquals(ChatEngine.Step.MENU, next.engine.step());
    }
    @Test public void logoutOrDeletedFirebaseUserWipesAndNewLoginStartsFresh() {
        ChatSession first = ChatSession.forUser("a"); first.engine.send("solar"); ChatSession.syncUser(null);
        assertFalse(first.active()); assertTrue(first.engine.messages().isEmpty());
        ChatSession reopened = ChatSession.forUser("a"); assertNotSame(first, reopened);
        assertEquals(ChatEngine.Step.MENU, reopened.engine.step());
        ChatSession.clear(); assertFalse(reopened.active());
    }
}

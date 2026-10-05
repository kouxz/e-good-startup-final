package com.projeto.egoodapp.chat;

/** Process-memory only. No Activity references or serialized conversation state. */
public final class ChatSession {
    private static ChatSession current;
    public final String userId;
    public final ChatEngine engine = new ChatEngine();
    private String draft = "";
    private boolean active = true;
    private ChatSession(String uid) { userId = uid; }
    public static synchronized ChatSession forUser(String uid) {
        if (uid == null || uid.isEmpty()) throw new IllegalArgumentException("Missing user");
        if (current == null || !current.userId.equals(uid)) { clear(); current = new ChatSession(uid); }
        return current;
    }
    public static synchronized void syncUser(String uid) {
        if (current != null && !current.userId.equals(uid)) clear();
    }
    public static synchronized void clear() {
        if (current != null) {
            current.active = false; current.draft = ""; current.engine.clear(); current = null;
        }
    }
    public boolean active() { return active; }
    public String draft() { return draft; }
    public void draft(String value) { if (active) draft = value; }
}

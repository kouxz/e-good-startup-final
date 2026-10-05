package com.projeto.egoodapp.chat;

/** Plain text only: messages are never interpreted as HTML or stored on disk. */
public final class ChatMessage {
    public static final int TYPE_USER = 1, TYPE_BOT = 2;
    private final String text;
    private final int type;
    public ChatMessage(String text, int type) { this.text = text; this.type = type; }
    public String getText() { return text; }
    public int getType() { return type; }
}

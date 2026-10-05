package com.projeto.egoodapp.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;

public final class ChatViewModel extends ViewModel {
    private ChatSession session;
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>();
    public void bind(String uid) { session = ChatSession.forUser(uid); refresh(); }
    public LiveData<List<ChatMessage>> messages() { return messages; }
    public void start(String topic) {
        if (session == null || !session.active()) return;
        session.engine.start(topic); session.draft(""); refresh();
    }
    public void send(String input) {
        if (session == null || !session.active()) return;
        session.engine.send(input); session.draft(""); refresh();
    }
    public String draft() { return session == null ? "" : session.draft(); }
    public void draft(String value) { if (session != null) session.draft(value); }
    private void refresh() { messages.setValue(session.engine.messages()); }
}

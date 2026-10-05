package com.projeto.egoodapp.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.local.LocalSession;
import com.projeto.egoodapp.security.SessionTimeoutController;
import com.projeto.egoodapp.views.common.session.AuthenticatedActivity;

public final class ChatActivity extends AuthenticatedActivity {
    private ChatViewModel model;
    private EditText input;
    private MaterialButton send;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AccountProfile profile = LocalSession.current(this, "pessoa");
        if (profile == null) { LocalSession.logout(this); return; }
        setContentView(R.layout.activity_chat);
        model = new ViewModelProvider(this).get(ChatViewModel.class); model.bind(profile.uid);
        RecyclerView list = findViewById(R.id.rvChat);
        LinearLayoutManager layout = new LinearLayoutManager(this); layout.setStackFromEnd(true);
        list.setLayoutManager(layout);
        ChatAdapter adapter = new ChatAdapter(); list.setAdapter(adapter);
        input = findViewById(R.id.etMessage); send = findViewById(R.id.btnSend);
        input.setText(model.draft()); input.setSelection(input.length());
        send.setEnabled(!input.getText().toString().trim().isEmpty());
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                model.draft(s.toString()); send.setEnabled(!s.toString().trim().isEmpty());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        model.messages().observe(this, messages -> adapter.submitList(messages, () -> {
            if (!messages.isEmpty()) list.scrollToPosition(messages.size() - 1);
        }));
        send.setOnClickListener(v -> send());
        input.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEND) { send(); return true; } return false;
        });
        findViewById(R.id.btnChatBack).setOnClickListener(v -> finish());
        findViewById(R.id.chatSuggestCompare).setOnClickListener(v -> suggest("Comparar veículos"));
        findViewById(R.id.chatSuggestCustom).setOnClickListener(v -> suggest("Meu carro"));
        findViewById(R.id.chatSuggestSolar).setOnClickListener(v -> suggest("Solar"));
    }
    private void suggest(String text) {
        if (!SessionTimeoutController.get(this).interact(this)) return;
        model.start(text); input.setText("");
    }
    private void send() {
        if (!SessionTimeoutController.get(this).interact(this)) return;
        String text = input.getText().toString().trim();
        if (!text.isEmpty()) { model.send(text); input.setText(""); }
    }
}

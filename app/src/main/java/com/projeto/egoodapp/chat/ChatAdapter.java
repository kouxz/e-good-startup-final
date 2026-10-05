package com.projeto.egoodapp.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.projeto.egoodapp.R;

public final class ChatAdapter extends ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder> {
    public ChatAdapter() {
        super(new DiffUtil.ItemCallback<>() {
            @Override public boolean areItemsTheSame(@NonNull ChatMessage old, @NonNull ChatMessage next) { return old == next; }
            @Override public boolean areContentsTheSame(@NonNull ChatMessage old, @NonNull ChatMessage next) {
                return old.getType() == next.getType() && old.getText().equals(next.getText());
            }
        });
    }
    @NonNull @Override public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new ChatViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        boolean user = message.getType() == ChatMessage.TYPE_USER;
        holder.user.setVisibility(user ? View.VISIBLE : View.GONE);
        holder.bot.setVisibility(user ? View.GONE : View.VISIBLE);
        // Clear both views when recycling so stale content is never exposed to accessibility.
        holder.userText.setText(user ? message.getText() : "");
        holder.botText.setText(user ? "" : message.getText());
    }
    static final class ChatViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView user, bot;
        final TextView userText, botText;
        ChatViewHolder(View view) {
            super(view); user = view.findViewById(R.id.containerUser); bot = view.findViewById(R.id.containerBot);
            userText = view.findViewById(R.id.tvUserMessage); botText = view.findViewById(R.id.tvBotMessage);
        }
    }
}

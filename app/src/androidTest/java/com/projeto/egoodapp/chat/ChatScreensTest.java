package com.projeto.egoodapp.chat;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.views.privacy.PrivacyUi;
import com.projeto.egoodapp.views.auth.RegisterActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ChatScreensTest {
    @Test public void homeAndChatLayoutsExposeAccessibleControlsAndBoundedInput() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            scenario.onActivity(activity -> {
                activity.setContentView(R.layout.activity_home);
                assertEquals("Abrir assistente e-good", activity.findViewById(R.id.btnHomeChat).getContentDescription());
                activity.setContentView(R.layout.activity_chat);
                assertNotNull(activity.findViewById(R.id.rvChat));
                assertNotNull(activity.findViewById(R.id.btnChatBack));
                assertNotNull(activity.findViewById(R.id.chatSuggestCompare));
                EditText input = activity.findViewById(R.id.etMessage);
                char[] longInput = new char[600]; java.util.Arrays.fill(longInput, 'a');
                input.setText(new String(longInput)); assertEquals(500, input.length());
                assertFalse(input.isSaveEnabled());
                View bubbles = activity.getLayoutInflater().inflate(R.layout.item_chat_message, null);
                assertNotNull(bubbles.findViewById(R.id.tvBotMessage));
                assertNotNull(bubbles.findViewById(R.id.tvUserMessage));
            });
        }
    }
    @Test public void activeConversationSurvivesRotationAndReopeningWithDraft() {
        Context context = ApplicationProvider.getApplicationContext();
        AccountProfile profile = PrivacyUi.current(context);
        org.junit.Assume.assumeTrue("Requires a signed-in Person test account", profile != null && !profile.isDealer());
        ChatSession.clear();
        try {
            try (ActivityScenario<ChatActivity> scenario = ActivityScenario.launch(ChatActivity.class)) {
                scenario.onActivity(activity -> {
                    activity.findViewById(R.id.chatSuggestCompare).performClick();
                    ((EditText) activity.findViewById(R.id.etMessage)).setText("1");
                    activity.findViewById(R.id.btnSend).performClick();
                    ((EditText) activity.findViewById(R.id.etMessage)).setText("Dolphin");
                    assertEquals(ChatEngine.Step.ELECTRIC, ChatSession.forUser(profile.uid).engine.step());
                });
                scenario.recreate();
                scenario.onActivity(activity -> {
                    assertEquals("Dolphin", ((EditText) activity.findViewById(R.id.etMessage)).getText().toString());
                    assertTrue(((RecyclerView) activity.findViewById(R.id.rvChat)).getAdapter() instanceof ChatAdapter);
                    activity.findViewById(R.id.btnChatBack).performClick();
                });
            }
            try (ActivityScenario<ChatActivity> reopened = ActivityScenario.launch(ChatActivity.class)) {
                reopened.onActivity(activity -> {
                    assertEquals("Dolphin", ((EditText) activity.findViewById(R.id.etMessage)).getText().toString());
                    assertEquals(ChatEngine.Step.ELECTRIC, ChatSession.forUser(profile.uid).engine.step());
                });
            }
        } finally { ChatSession.clear(); }
    }
}

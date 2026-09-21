package com.projeto.egoodapp.views;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.views.user.LoginActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class LoginAndRegisterScreensTest {
    @Test public void accountSelectionSurvivesRotationAndOnlyOneCardIsChecked() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(((MaterialCardView) activity.findViewById(R.id.cardPessoa)).isChecked());
                assertFalse(((MaterialCardView) activity.findViewById(R.id.cardConcessionaria)).isChecked());
                activity.findViewById(R.id.cardConcessionaria).performClick();
                assertTrue(((MaterialCardView) activity.findViewById(R.id.cardConcessionaria)).isChecked());
                assertTrue(activity.findViewById(R.id.btnProximo) instanceof MaterialButton);
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertFalse(((MaterialCardView) activity.findViewById(R.id.cardPessoa)).isChecked());
                assertTrue(((MaterialCardView) activity.findViewById(R.id.cardConcessionaria)).isChecked());
                activity.findViewById(R.id.cardPessoa).performClick();
                assertTrue(((MaterialCardView) activity.findViewById(R.id.cardPessoa)).isChecked());
                assertFalse(((MaterialCardView) activity.findViewById(R.id.cardConcessionaria)).isChecked());
            });
        }
    }

    @Test public void missingGoogleConfigurationKeepsPasswordLoginAvailable() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                assumeTrue(activity.getResources().getIdentifier("default_web_client_id", "string", activity.getPackageName()) == 0);
                activity.findViewById(R.id.btnGoogle).performClick();
                assertTrue(activity.findViewById(R.id.btnEntrar).isEnabled());
                assertTrue(activity.findViewById(R.id.editEmail).isEnabled());
                assertTrue(activity.findViewById(R.id.editSenha).isEnabled());
            });
            onView(withText("O login com Google está indisponível no momento. Use seu e-mail e senha."))
                    .check(matches(isDisplayed()));
        }
    }
}

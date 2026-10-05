package com.projeto.egoodapp.views.auth;

import android.graphics.Color;
import android.text.method.PasswordTransformationMethod;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.views.auth.DealerRegisterActivity;
import com.projeto.egoodapp.views.auth.LoginActivity;
import com.projeto.egoodapp.views.auth.UserRegisterActivity;
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

    @Test public void loginPasswordStartsMaskedAndKeepsTheVisibilityToggle() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                TextInputEditText password = activity.findViewById(R.id.editSenha);
                TextInputLayout layout = activity.findViewById(R.id.inputLayoutSenha);
                password.setText("1234");
                assertTrue(password.getTransformationMethod()
                        instanceof PasswordTransformationMethod);
                assertEquals("••••", password.getTransformationMethod()
                        .getTransformation(password.getText(), password).toString());
                assertEquals(TextInputLayout.END_ICON_PASSWORD_TOGGLE, layout.getEndIconMode());

                activity.findViewById(com.google.android.material.R.id.text_input_end_icon)
                        .performClick();
                assertNull(password.getTransformationMethod());

                activity.findViewById(com.google.android.material.R.id.text_input_end_icon)
                        .performClick();
                assertTrue(password.getTransformationMethod()
                        instanceof PasswordTransformationMethod);
                assertEquals("••••", password.getTransformationMethod()
                        .getTransformation(password.getText(), password).toString());
            });
        }
    }

    @Test public void userRegistrationUsesLoginFieldsAndHasNoLocationControls() {
        try (ActivityScenario<UserRegisterActivity> scenario = ActivityScenario.launch(UserRegisterActivity.class)) {
            scenario.onActivity(activity -> {
                TextInputLayout password = activity.findViewById(R.id.inputLayoutUserPassword);
                TextInputLayout confirmation = activity.findViewById(R.id.inputLayoutUserConfirmPassword);
                assertEquals(TextInputLayout.BOX_BACKGROUND_OUTLINE, password.getBoxBackgroundMode());
                assertNotEquals(Color.BLACK, password.getBoxBackgroundColor());
                assertEquals(TextInputLayout.END_ICON_PASSWORD_TOGGLE, password.getEndIconMode());
                assertEquals(TextInputLayout.END_ICON_PASSWORD_TOGGLE, confirmation.getEndIconMode());
                assertFullyMasked(activity.findViewById(R.id.editSenha));
                assertFullyMasked(activity.findViewById(R.id.editConfSenha));
                assertNotNull(activity.findViewById(R.id.editNome));
                assertNotNull(activity.findViewById(R.id.editTelefoneUsuario));
                assertEquals(0, activity.getResources().getIdentifier(
                        "btnAutoLocation", "id", activity.getPackageName()));
            });
        }
    }

    @Test public void dealerRegistrationKeepsAllSectionsAndLocationActions() {
        try (ActivityScenario<DealerRegisterActivity> scenario = ActivityScenario.launch(DealerRegisterActivity.class)) {
            scenario.onActivity(activity -> {
                TextInputLayout password = activity.findViewById(R.id.inputLayoutDealerPassword);
                TextInputLayout confirmation = activity.findViewById(R.id.inputLayoutDealerConfirmPassword);
                assertEquals(TextInputLayout.BOX_BACKGROUND_OUTLINE, password.getBoxBackgroundMode());
                assertNotEquals(Color.BLACK, password.getBoxBackgroundColor());
                assertEquals(TextInputLayout.END_ICON_PASSWORD_TOGGLE, password.getEndIconMode());
                assertEquals(TextInputLayout.END_ICON_PASSWORD_TOGGLE, confirmation.getEndIconMode());
                assertFullyMasked(activity.findViewById(R.id.editSenhaEmpresa));
                assertFullyMasked(activity.findViewById(R.id.editConfSenhaEmpresa));
                assertNotNull(activity.findViewById(R.id.editNomeEmpresa));
                assertNotNull(activity.findViewById(R.id.editCnpj));
                assertNotNull(activity.findViewById(R.id.editEndereco));
                assertNotNull(activity.findViewById(R.id.editEstado));
                assertNotNull(activity.findViewById(R.id.editCep));
                assertTrue(activity.findViewById(R.id.btnAutoLocationConcessionaria) instanceof MaterialButton);
                assertTrue(activity.findViewById(R.id.btnManualLocationConcessionaria) instanceof MaterialButton);
            });
            onView(withText("Dados da empresa")).check(matches(isDisplayed()));
            onView(withText("Dados de acesso")).check(matches(isDisplayed()));
            onView(withText("Localização")).check(matches(isDisplayed()));
        }
    }

    private static void assertFullyMasked(TextInputEditText field) {
        field.setText("1234");
        assertTrue(field.getTransformationMethod() instanceof PasswordTransformationMethod);
        assertEquals("••••", field.getTransformationMethod()
                .getTransformation(field.getText(), field).toString());
    }
}

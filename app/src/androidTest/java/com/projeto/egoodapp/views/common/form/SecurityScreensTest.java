package com.projeto.egoodapp.views.common.form;

import com.projeto.egoodapp.views.auth.RegisterActivity;

import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.material.textfield.TextInputEditText;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.views.privacy.PrivacyUi;
import com.projeto.egoodapp.views.auth.DealerRegisterActivity;
import com.projeto.egoodapp.views.auth.UserRegisterActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SecurityScreensTest {
    @Test public void personPasswordChecklistAndFictionalEmailSurviveRotation() {
        try (ActivityScenario<UserRegisterActivity> scenario = ActivityScenario.launch(UserRegisterActivity.class)) {
            scenario.onActivity(activity -> {
                ((TextInputEditText) activity.findViewById(R.id.editEmail)).setText("teste@exemplo.invalid");
                ((TextInputEditText) activity.findViewById(R.id.editTelefoneUsuario)).setText("123456789012345");
                assertEquals(11, ((TextInputEditText) activity.findViewById(R.id.editTelefoneUsuario)).length());
                ((TextInputEditText) activity.findViewById(R.id.editSenha)).setText("Teste123!");
                assertTrue(((TextView) activity.findViewById(R.id.passwordRuleUpper)).getContentDescription().toString().endsWith("atendido"));
                assertTrue(((TextView) activity.findViewById(R.id.passwordRuleSpecial)).getContentDescription().toString().endsWith("atendido"));
            });
            scenario.recreate();
            scenario.onActivity(activity -> assertEquals("teste@exemplo.invalid",
                    ((TextInputEditText) activity.findViewById(R.id.editEmail)).getText().toString()));
        }
    }
    @Test public void companyCnpjIsLengthLimitedWithoutVerifyingItsExistence() {
        try (ActivityScenario<DealerRegisterActivity> scenario = ActivityScenario.launch(DealerRegisterActivity.class)) {
            scenario.onActivity(activity -> {
                TextInputEditText cnpj = activity.findViewById(R.id.editCnpj);
                cnpj.setText("111111111111111111"); assertEquals("11111111111111", cnpj.getText().toString());
                ((TextInputEditText) activity.findViewById(R.id.editSenhaEmpresa)).setText("fraca");
                assertTrue(((TextView) activity.findViewById(R.id.passwordRuleUpper)).getContentDescription().toString().endsWith("pendente"));
                activity.findViewById(R.id.btnAutoLocationConcessionaria).performClick();
            });
            onView(withText("Uso opcional da localização")).check(matches(isDisplayed()));
            scenario.recreate();
            onView(withText("Continuar sem localização")).check(matches(isDisplayed()));
        }
    }
    @Test public void bothAccountLayoutsExposePrivacyAndRightsAndDialogRestores() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            scenario.onActivity(activity -> {
                activity.setContentView(R.layout.activity_profile);
                assertNotNull(activity.findViewById(R.id.btnProfilePrivacy));
                assertNotNull(activity.findViewById(R.id.btnProfileLgpd));
                activity.setContentView(R.layout.section_dealer_settings);
                assertNotNull(activity.findViewById(R.id.btnDealerPrivacy));
                assertNotNull(activity.findViewById(R.id.btnDealerLgpd));
                PrivacyUi.showRights(activity);
            });
            onView(withText("LGPD e seus direitos")).check(matches(isDisplayed()));
            scenario.recreate();
            onView(withText("LGPD e seus direitos")).check(matches(isDisplayed()));
        }
    }
}

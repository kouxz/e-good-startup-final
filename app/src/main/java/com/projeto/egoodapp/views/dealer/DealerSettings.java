package com.projeto.egoodapp.views.dealer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.theme.AppThemeController;
import com.projeto.egoodapp.views.account.AccountDeletionUi;
import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.local.LocalRepository;

final class DealerSettings {
    private final AppCompatActivity activity;
    private final String ownerId;
    private final LocalRepository repository;
    private final SwitchMaterial notifications, interests, darkMode;
    private boolean loading;

    DealerSettings(AppCompatActivity activity, String ownerId, Runnable logout) {
        this.activity = activity; this.ownerId = ownerId; repository = LocalRepository.get(activity);
        notifications = activity.findViewById(R.id.switchDealerNotifications);
        interests = activity.findViewById(R.id.switchDealerInterests);
        darkMode = activity.findViewById(R.id.switchDealerDarkMode);
        notifications.setOnCheckedChangeListener((button, checked) -> { if (!loading) save(true, checked); });
        interests.setOnCheckedChangeListener((button, checked) -> { if (!loading) save(false, checked); });
        darkMode.setOnCheckedChangeListener((button, checked) -> {
            if (!loading && checked != AppThemeController.isDarkModeEnabled(activity)) {
                AppThemeController.setDarkModeEnabled(activity, checked);
            }
        });
        activity.findViewById(R.id.btnDealerChangePassword).setOnClickListener(view -> changePassword());
        activity.findViewById(R.id.btnDealerHelp).setOnClickListener(view -> dialog("Central de Ajuda",
                "• Publique veículos pela opção Adicionar.\n\n• Acompanhe contatos em Interessados e altere o status pelo selo.\n\n• Atualize os dados públicos da empresa em Perfil."));
        activity.findViewById(R.id.btnDealerContact).setOnClickListener(view -> contact());
        activity.findViewById(R.id.btnDealerTerms).setOnClickListener(view -> dialog("Termos de Uso",
                "Este protótipo armazena perfis, veículos, interesses e preferências somente neste aparelho. Os dados não são sincronizados entre dispositivos. Use informações verdadeiras e publique apenas veículos que sua empresa está autorizada a anunciar."));
        activity.findViewById(R.id.btnDealerPrivacy).setOnClickListener(view -> com.projeto.egoodapp.views.privacy.PrivacyUi.showPolicy(activity));
        activity.findViewById(R.id.btnDealerLgpd).setOnClickListener(view -> com.projeto.egoodapp.views.privacy.PrivacyUi.showRights(activity));
        activity.findViewById(R.id.btnDealerDevelopers).setOnClickListener(view -> dialog(
                "Desenvolvedores", activity.getString(R.string.developers_list)));
        activity.findViewById(R.id.btnSettingsLogout).setOnClickListener(view -> logout.run());
        new AccountDeletionUi(activity, activity.findViewById(R.id.btnDealerDeleteAccount), true);
        refresh();
    }
    void refresh() {
        AccountProfile profile = repository.account(ownerId); loading = true;
        notifications.setChecked(profile == null || profile.notificationsEnabled());
        interests.setChecked(profile == null || profile.newInterestsEnabled());
        darkMode.setChecked(AppThemeController.isDarkModeEnabled(activity));
        loading = false;
    }
    private void save(boolean general, boolean checked) {
        AccountProfile profile = repository.account(ownerId);
        if (profile == null || !profile.isDealer()) return;
        if (general) profile.notificationsEnabled = checked; else profile.newInterestsEnabled = checked;
        repository.saveAccount(profile);
    }
    private void changePassword() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            dialog("Alterar senha", "Não há um e-mail disponível para esta conta."); return;
        }
        boolean passwordProvider = false;
        for (UserInfo info : user.getProviderData()) if ("password".equals(info.getProviderId())) passwordProvider = true;
        if (!passwordProvider) {
            dialog("Alterar senha", "Esta conta usa o Google. Altere a senha nas configurações da sua Conta Google."); return;
        }
        activity.findViewById(R.id.btnDealerChangePassword).setEnabled(false);
        FirebaseAuth.getInstance().setLanguageCode("pt");
        FirebaseAuth.getInstance().sendPasswordResetEmail(user.getEmail()).addOnCompleteListener(activity, task -> {
            activity.findViewById(R.id.btnDealerChangePassword).setEnabled(true);
            com.projeto.egoodapp.data.local.SecurityAudit.record(activity,
                    com.projeto.egoodapp.data.model.SecurityAuditEvent.Type.PASSWORD_RESET,
                    task.isSuccessful() ? com.projeto.egoodapp.data.model.SecurityAuditEvent.Result.SUCCESS
                    : com.projeto.egoodapp.data.model.SecurityAuditEvent.Result.FAILURE, user.getUid());
            if (task.isSuccessful() || com.projeto.egoodapp.views.auth.AuthErrorMessages.invalidCredentials(task.getException()))
                dialog("Solicitação concluída", "Se esse e-mail estiver cadastrado, você receberá um link para redefinir a senha. Verifique também a caixa de spam.");
            else dialog("Não foi possível alterar a senha", com.projeto.egoodapp.views.auth.AuthErrorMessages.recovery(task.getException()));
        });
    }
    private void contact() {
        Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "Suporte e-good Business")
                .putExtra(Intent.EXTRA_TEXT, "Olá, preciso de ajuda com o e-good Business.\n\nDescreva aqui sua dúvida:");
        try { activity.startActivity(Intent.createChooser(share, "Fale Conosco")); }
        catch (ActivityNotFoundException error) { Toast.makeText(activity, "Nenhum aplicativo compatível foi encontrado", Toast.LENGTH_LONG).show(); }
    }
    private void dialog(String title, String message) {
        new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(activity, R.style.ThemeOverlay_EGood_LocalDialog)
                .setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }
}

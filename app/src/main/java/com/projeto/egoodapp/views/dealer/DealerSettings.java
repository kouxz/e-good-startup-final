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
import com.projeto.egoodapp.data.local.AccountProfile;
import com.projeto.egoodapp.data.local.LocalRepository;

final class DealerSettings {
    private final AppCompatActivity activity;
    private final String ownerId;
    private final LocalRepository repository;
    private final SwitchMaterial notifications, interests;
    private boolean loading;

    DealerSettings(AppCompatActivity activity, String ownerId, Runnable logout) {
        this.activity = activity; this.ownerId = ownerId; repository = LocalRepository.get(activity);
        notifications = activity.findViewById(R.id.switchDealerNotifications);
        interests = activity.findViewById(R.id.switchDealerInterests);
        notifications.setOnCheckedChangeListener((button, checked) -> { if (!loading) save(true, checked); });
        interests.setOnCheckedChangeListener((button, checked) -> { if (!loading) save(false, checked); });
        activity.findViewById(R.id.btnDealerChangePassword).setOnClickListener(view -> changePassword());
        activity.findViewById(R.id.btnDealerHelp).setOnClickListener(view -> dialog("Central de Ajuda",
                "• Publique veículos pela opção Adicionar.\n\n• Acompanhe contatos em Interessados e altere o status pelo selo.\n\n• Atualize os dados públicos da empresa em Perfil."));
        activity.findViewById(R.id.btnDealerContact).setOnClickListener(view -> contact());
        activity.findViewById(R.id.btnDealerTerms).setOnClickListener(view -> dialog("Termos de Uso",
                "Este protótipo armazena perfis, veículos, interesses e preferências somente neste aparelho. Os dados não são sincronizados entre dispositivos. Use informações verdadeiras e publique apenas veículos que sua empresa está autorizada a anunciar."));
        activity.findViewById(R.id.btnSettingsLogout).setOnClickListener(view -> logout.run());
        refresh();
    }
    void refresh() {
        AccountProfile profile = repository.account(ownerId); loading = true;
        notifications.setChecked(profile == null || profile.notificationsEnabled());
        interests.setChecked(profile == null || profile.newInterestsEnabled()); loading = false;
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
            if (task.isSuccessful()) dialog("E-mail enviado", "Enviamos as instruções de alteração de senha. Verifique também a caixa de spam.");
            else dialog("Não foi possível alterar a senha", "Verifique sua conexão e tente novamente.");
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
        new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_EGood_LocalDialog)
                .setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }
}

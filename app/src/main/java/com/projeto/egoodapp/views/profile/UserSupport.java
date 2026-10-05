package com.projeto.egoodapp.views.profile;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.projeto.egoodapp.R;

final class UserSupport {
    private final AppCompatActivity activity;

    UserSupport(AppCompatActivity activity) {
        this.activity = activity;
        activity.findViewById(R.id.btnProfileHelp).setOnClickListener(view -> dialog(
                "Central de Ajuda",
                "• Consulte e pesquise veículos na aba Veículos.\n\n"
                        + "• Compare custos e simule sua quilometragem na aba Comparar.\n\n"
                        + "• Faça uma estimativa fotovoltaica na aba Solar.\n\n"
                        + "• Toque no cabeçalho do Perfil para atualizar seus dados."));
        activity.findViewById(R.id.btnProfileContact).setOnClickListener(view -> contact());
        activity.findViewById(R.id.btnProfileTerms).setOnClickListener(view -> dialog(
                "Termos de Uso",
                "Este aplicativo é um protótipo. Perfis, veículos, interesses e preferências "
                        + "são armazenados localmente neste aparelho e não são sincronizados entre dispositivos."));
        activity.findViewById(R.id.btnProfilePrivacy).setOnClickListener(view -> com.projeto.egoodapp.views.privacy.PrivacyUi.showPolicy(activity));
        activity.findViewById(R.id.btnProfileLgpd).setOnClickListener(view -> com.projeto.egoodapp.views.privacy.PrivacyUi.showRights(activity));
        activity.findViewById(R.id.btnProfileDevelopers).setOnClickListener(view -> dialog(
                "Desenvolvedores", activity.getString(R.string.developers_list)));
    }

    static String developersText() {
        return "Fernanda Falcão Kedouk Simões\n"
                + "Giovanni Pereira Valente\n"
                + "Kauã Garcia Francisco\n"
                + "Maria Eduarda Souza Santos\n"
                + "Nicole Kerne";
    }

    private void contact() {
        Intent share = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "Suporte e-good")
                .putExtra(Intent.EXTRA_TEXT, "Olá, preciso de ajuda com o e-good.\n\nDescreva aqui sua dúvida:");
        try {
            activity.startActivity(Intent.createChooser(share, "Fale Conosco"));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(activity, "Nenhum aplicativo compatível foi encontrado", Toast.LENGTH_LONG).show();
        }
    }

    private void dialog(String title, String message) {
        new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(activity, R.style.ThemeOverlay_EGood_LocalDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}

package com.projeto.egoodapp.views.dealership;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.model.DealerRatingSummary;
import com.projeto.egoodapp.data.local.LocalRepository;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class DealerRatingDialogs {
    private DealerRatingDialogs() {}

    public static String summary(double average, int count) {
        if (count == 0) return "Sem avaliações";
        DecimalFormat format = new DecimalFormat("0.0",
                new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR")));
        return format.format(average) + " (" + count + (count == 1 ? " avaliação)" : " avaliações)");
    }

    public static void show(Activity activity, String dealerKey, String dealerName, Runnable changed) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        LocalRepository repository = LocalRepository.get(activity);
        AccountProfile user = firebaseUser == null ? null : repository.account(firebaseUser.getUid());
        if (user == null || user.isDealer()) {
            Toast.makeText(activity, "Entre com uma conta Pessoa para avaliar", Toast.LENGTH_LONG).show();
            return;
        }

        DealerRatingSummary current = repository.dealerRating(dealerKey, user.uid);
        int padding = Math.round(24 * activity.getResources().getDisplayMetrics().density);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(padding, Math.round(padding / 2f), padding, 0);

        TextView explanation = new TextView(activity);
        explanation.setText("Escolha uma nota de 1 a 5 estrelas.");
        explanation.setTextSize(14);
        explanation.setGravity(Gravity.CENTER);
        content.addView(explanation, new LinearLayout.LayoutParams(-1, -2));

        RatingBar ratingBar = new RatingBar(activity);
        ratingBar.setIsIndicator(false);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1);
        ratingBar.setRating(current.userScore == null ? 0 : current.userScore);
        LinearLayout.LayoutParams ratingParams = new LinearLayout.LayoutParams(-2, -2);
        ratingParams.topMargin = Math.round(12 * activity.getResources().getDisplayMetrics().density);
        content.addView(ratingBar, ratingParams);

        MaterialAlertDialogBuilder builder = new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(activity)
                .setTitle("Avaliar " + dealerName)
                .setView(content)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null);
        if (current.userScore != null) {
            builder.setNeutralButton("Remover avaliação", (dialog, which) -> {
                repository.removeDealerRating(user.uid, dealerKey);
                changed.run();
            });
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    int score = Math.round(ratingBar.getRating());
                    if (score < 1) {
                        Toast.makeText(activity, "Selecione pelo menos uma estrela", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repository.rateDealer(user.uid, dealerKey, score);
                    changed.run();
                    dialog.dismiss();
                }));
        dialog.show();
    }
}

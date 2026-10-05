package com.projeto.egoodapp.views.dealer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.model.DealerPerformance;
import com.projeto.egoodapp.data.model.DealerRatingSummary;
import com.projeto.egoodapp.data.model.Interest;
import com.projeto.egoodapp.data.local.InterestWorkflow;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.LocalSession;
import com.projeto.egoodapp.views.account.LocalProfileForms;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class DealerSections {
    private static final String STATE_FILTER = "dealerContactFilter";
    private static final String STATE_PENDING_CONTACT = "dealerPendingContact";

    private final AppCompatActivity activity;
    private final String owner;
    private final LocalRepository repo;
    private final Runnable openContacts;
    private final ActivityResultLauncher<Intent> contactLauncher;
    private String filter = "Todos";
    private String pendingContactId;

    DealerSections(AppCompatActivity activity, String owner, Runnable openContacts) {
        this.activity = activity;
        this.owner = owner;
        this.repo = LocalRepository.get(activity);
        this.openContacts = openContacts;
        contactLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> offerMarkAsContacted());
    }

    void restore(Bundle state) {
        if (state == null) return;
        String restoredFilter = state.getString(STATE_FILTER);
        if ("Todos".equals(restoredFilter)
                || InterestWorkflow.STATUSES.contains(restoredFilter)) {
            filter = restoredFilter;
        }
        pendingContactId = state.getString(STATE_PENDING_CONTACT);
    }

    void save(Bundle state) {
        state.putString(STATE_FILTER, filter);
        if (pendingContactId != null) state.putString(STATE_PENDING_CONTACT, pendingContactId);
    }

    void dashboard() {
        AccountProfile profile = repo.account(owner);
        String name = fallback(profile.name, "Minha concessionária");
        label(R.id.tvDashboardGreeting, "Bem-vindo(a), " + name);
        label(R.id.tvDashboardCompany, name);
        List<Interest> interests = repo.interests(owner);
        label(R.id.tvDashboardVehicles, String.valueOf(repo.dealerVehicles(owner).size()));
        label(R.id.tvDashboardInterests, String.valueOf(interests.size()));
        label(R.id.tvDashboardInContact, String.valueOf(interests.stream()
                .filter(i -> InterestWorkflow.STATUS_IN_CONTACT.equals(i.status)).count()));
        long[] period = currentMonth();
        DealerPerformance performance = repo.performance(owner, period[0], period[1]);
        label(R.id.tvDashboardSales, String.valueOf(performance.sales));
        label(R.id.tvDashboardViews, String.valueOf(performance.views));
        label(R.id.tvDashboardMonthlyContacts, String.valueOf(performance.contacts));
        label(R.id.tvDashboardConversion, performance.conversionPercent + "%");
        progress(R.id.progressDashboardViews, performance.views,
                DealerPerformance.nextMilestone(performance.views));
        progress(R.id.progressDashboardContacts, performance.contacts,
                DealerPerformance.nextMilestone(performance.contacts));
        progress(R.id.progressDashboardConversion, performance.conversionPercent, 100);

        LinearLayout list = activity.findViewById(R.id.dashboardRecentInterests);
        list.removeAllViews();
        for (Interest interest : interests.subList(0, Math.min(3, interests.size()))) {
            View card = activity.getLayoutInflater().inflate(
                    R.layout.item_dealer_interest, list, false);
            bindIdentity(card, interest);
            TextView status = card.findViewById(R.id.tvInterestStatus);
            bindStatus(status, interest.status);
            bindOutcome(card.findViewById(R.id.tvInterestOutcome), interest);
            status.setOnClickListener(view -> openContacts.run());
            card.findViewById(R.id.tvInterestPhone).setVisibility(View.GONE);
            card.findViewById(R.id.tvInterestDate).setVisibility(View.GONE);
            ((View) card.findViewById(R.id.btnInterestCall).getParent()).setVisibility(View.GONE);
            card.findViewById(R.id.btnInterestStatusAction).setVisibility(View.GONE);
            card.setOnClickListener(view -> openContacts.run());
            list.addView(card);
        }
        if (interests.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText("Nenhum interessado recebido");
            empty.setTextColor(ContextCompat.getColor(activity, R.color.app_text_secondary));
            list.addView(empty);
        }
    }

    void contacts() {
        List<Interest> contacts = repo.interests(owner);
        label(R.id.tvInterestCount, contacts.size()
                + (contacts.size() == 1 ? " contato recebido" : " contatos recebidos"));
        bindContactSummary();
        bindFilters(contacts);

        LinearLayout list = activity.findViewById(R.id.interestsList);
        list.removeAllViews();
        for (Interest interest : contacts) {
            if (!"Todos".equals(filter) && !filter.equals(interest.status)) continue;
            View card = activity.getLayoutInflater().inflate(
                    R.layout.item_dealer_interest, list, false);
            bindContactCard(card, interest);
            list.addView(card);
        }
        if (list.getChildCount() == 0) {
            TextView empty = new TextView(activity);
            empty.setText("Nenhum interessado neste filtro");
            empty.setTextColor(ContextCompat.getColor(activity, R.color.app_text_secondary));
            empty.setPadding(0, 24, 0, 24);
            list.addView(empty);
        }
    }

    void profile() {
        AccountProfile profile = repo.account(owner);
        label(R.id.tvCompanyName, fallback(profile.name, "Minha concessionária"));
        label(R.id.tvCompanyCnpj, "CNPJ: " + fallback(profile.cnpj, "Não informado"));
        label(R.id.tvCompanyFieldName, fallback(profile.name, "Não informado"));
        label(R.id.tvCompanyPhone, fallback(profile.phone, "Não informado"));
        label(R.id.tvCompanyAddress, fallback(profile.address, "Não informado"));
        label(R.id.tvCompanyCity, profile.city.isEmpty()
                ? "Não informado" : profile.city + ", " + profile.state);
        label(R.id.tvCompanyDescription, fallback(
                profile.description, "Nenhuma descrição informada"));
        label(R.id.tvCompanyVehicleCount, String.valueOf(repo.dealerVehicles(owner).size()));
        long[] period = currentMonth();
        DealerPerformance performance = repo.performance(owner, period[0], period[1]);
        label(R.id.tvCompanySales, String.valueOf(performance.sales));
        DealerRatingSummary rating = repo.dealerRating("local:" + owner, null);
        label(R.id.tvCompanyRating, rating.count == 0 ? "—"
                : String.format(Locale.forLanguageTag("pt-BR"), "%.1f", rating.average));
        label(R.id.tvCompanyRatingLabel, rating.count == 0 ? "Sem avaliações"
                : rating.count + (rating.count == 1 ? " avaliação" : " avaliações"));
        activity.findViewById(R.id.btnEditCompany).setOnClickListener(view ->
                LocalProfileForms.edit(activity, profile, this::profile));
        activity.findViewById(R.id.btnCompanyLogout).setOnClickListener(view ->
                LocalSession.logout(activity));
    }

    private void bindContactSummary() {
        long[] period = currentMonth();
        DealerPerformance performance = repo.performance(owner, period[0], period[1]);
        label(R.id.tvContactsMonthlyCount, String.valueOf(performance.contacts));
        label(R.id.tvContactsMonthlySales, String.valueOf(performance.sales));
        label(R.id.tvContactsMonthlyConversion, performance.conversionPercent + "%");
    }

    private void bindFilters(List<Interest> contacts) {
        int total = contacts.size();
        int news = count(contacts, InterestWorkflow.STATUS_NEW);
        int inContact = count(contacts, InterestWorkflow.STATUS_IN_CONTACT);
        int finished = count(contacts, InterestWorkflow.STATUS_FINISHED);
        int[] ids = {R.id.filterInterest0, R.id.filterInterest1,
                R.id.filterInterest2, R.id.filterInterest3};
        String[] values = {"Todos", InterestWorkflow.STATUS_NEW,
                InterestWorkflow.STATUS_IN_CONTACT, InterestWorkflow.STATUS_FINISHED};
        String[] labels = {"Todos (" + total + ")", "Novos (" + news + ")",
                "Em contato (" + inContact + ")", "Finalizados (" + finished + ")"};
        for (int index = 0; index < ids.length; index++) {
            MaterialButton chip = activity.findViewById(ids[index]);
            String value = values[index];
            boolean active = filter.equals(value);
            chip.setText(labels[index]);
            chip.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity,
                    active ? R.color.primary_dark : R.color.app_surface_variant)));
            chip.setTextColor(active ? Color.WHITE
                    : ContextCompat.getColor(activity, R.color.app_text_secondary));
            chip.setOnClickListener(view -> {
                filter = value;
                contacts();
            });
        }
    }

    private void bindContactCard(View card, Interest interest) {
        bindIdentity(card, interest);
        ((TextView) card.findViewById(R.id.tvInterestPhone)).setText(
                fallback(interest.phone, "Telefone não informado"));
        ((TextView) card.findViewById(R.id.tvInterestDate)).setText(
                new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.forLanguageTag("pt-BR"))
                        .format(new Date(interest.createdAt)));
        TextView status = card.findViewById(R.id.tvInterestStatus);
        bindStatus(status, interest.status);
        status.setContentDescription("Status " + interest.status + " de " + interest.name);
        bindOutcome(card.findViewById(R.id.tvInterestOutcome), interest);

        MaterialButton update = card.findViewById(R.id.btnInterestStatusAction);
        update.setContentDescription("Atualizar andamento de " + interest.name);
        update.setOnClickListener(view -> showStatusDialog(interest));

        MaterialButton call = card.findViewById(R.id.btnInterestCall);
        String phone = interest.phone == null ? ""
                : interest.phone.replaceAll("[^0-9+]", "");
        call.setEnabled(!phone.isEmpty());
        call.setAlpha(phone.isEmpty() ? 0.45f : 1f);
        call.setOnClickListener(view -> launchContact(interest,
                new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))));

        MaterialButton email = card.findViewById(R.id.btnInterestEmail);
        boolean hasEmail = interest.email != null && !interest.email.trim().isEmpty();
        email.setEnabled(hasEmail);
        email.setAlpha(hasEmail ? 1f : 0.45f);
        email.setOnClickListener(view -> launchContact(interest,
                new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", interest.email, null))));
    }

    private void bindIdentity(View card, Interest interest) {
        ((TextView) card.findViewById(R.id.tvInterestAvatar)).setText(initials(interest.name));
        ((TextView) card.findViewById(R.id.tvInterestName)).setText(interest.name);
        ((TextView) card.findViewById(R.id.tvInterestVehicle)).setText(interest.vehicleName);
    }

    private void showStatusDialog(Interest interest) {
        new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(activity, R.style.ThemeOverlay_EGood_LocalDialog)
                .setTitle("Atualizar contato")
                .setItems(new String[]{"Novo", "Em contato", "Finalizar contato"},
                        (dialog, index) -> {
                            if (index == 0) {
                                updateInterest(interest, InterestWorkflow.STATUS_NEW, null);
                            } else if (index == 1) {
                                updateInterest(interest, InterestWorkflow.STATUS_IN_CONTACT, null);
                            } else {
                                showOutcomeDialog(interest);
                            }
                        })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showOutcomeDialog(Interest interest) {
        int currentSelection = InterestWorkflow.OUTCOME_SALE.equals(interest.outcome) ? 0
                : InterestWorkflow.OUTCOME_NO_SALE.equals(interest.outcome) ? 1 : -1;
        int[] selected = {currentSelection};
        AlertDialog dialog = new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(
                activity, R.style.ThemeOverlay_EGood_LocalDialog)
                .setTitle("Resultado do contato")
                .setSingleChoiceItems(new String[]{"Venda concluída", "Sem venda"},
                        currentSelection, (choiceDialog, index) -> selected[0] = index)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    if (selected[0] < 0) {
                        Toast.makeText(activity, "Escolha o resultado do contato",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateInterest(interest, InterestWorkflow.STATUS_FINISHED,
                            selected[0] == 0 ? InterestWorkflow.OUTCOME_SALE
                                    : InterestWorkflow.OUTCOME_NO_SALE);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void updateInterest(Interest interest, String status, String outcome) {
        boolean updated = repo.updateInterest(owner, interest.id, status, outcome);
        Toast.makeText(activity, updated ? "Andamento atualizado"
                : "Não foi possível atualizar o contato", Toast.LENGTH_SHORT).show();
        contacts();
    }

    private void launchContact(Interest interest, Intent intent) {
        pendingContactId = interest.id;
        try {
            contactLauncher.launch(intent);
        } catch (ActivityNotFoundException | SecurityException error) {
            pendingContactId = null;
            Toast.makeText(activity, "Nenhum aplicativo disponível para esta ação",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void offerMarkAsContacted() {
        if (pendingContactId == null) return;
        String interestId = pendingContactId;
        pendingContactId = null;
        Interest interest = findInterest(interestId);
        if (interest == null || !InterestWorkflow.STATUS_NEW.equals(interest.status)) return;
        new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(activity, R.style.ThemeOverlay_EGood_LocalDialog)
                .setTitle("Atualizar andamento?")
                .setMessage("Deseja marcar o contato de " + interest.name + " como Em contato?")
                .setNegativeButton("Agora não", null)
                .setPositiveButton("Marcar em contato", (dialog, which) ->
                        updateInterest(interest, InterestWorkflow.STATUS_IN_CONTACT, null))
                .show();
    }

    private Interest findInterest(String interestId) {
        for (Interest interest : repo.interests(owner)) {
            if (interestId.equals(interest.id)) return interest;
        }
        return null;
    }

    private void bindStatus(TextView view, String status) {
        view.setText(status);
        if (InterestWorkflow.STATUS_IN_CONTACT.equals(status)) {
            badge(view, R.color.contact_active_surface, R.color.contact_active_text);
        } else if (InterestWorkflow.STATUS_FINISHED.equals(status)) {
            badge(view, R.color.contact_finished_surface, R.color.contact_finished_text);
        } else {
            badge(view, R.color.contact_new_surface, R.color.contact_new_text);
        }
    }

    private void bindOutcome(TextView view, Interest interest) {
        String label = InterestWorkflow.outcomeLabel(interest.outcome);
        if (label.isEmpty() || !InterestWorkflow.STATUS_FINISHED.equals(interest.status)) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setText(label);
        view.setVisibility(View.VISIBLE);
        if (InterestWorkflow.OUTCOME_SALE.equals(interest.outcome)) {
            badge(view, R.color.contact_sale_surface, R.color.contact_sale_text);
        } else {
            badge(view, R.color.contact_no_sale_surface, R.color.contact_no_sale_text);
        }
    }

    private void badge(TextView view, int backgroundColor, int textColor) {
        GradientDrawable badge = new GradientDrawable();
        badge.setColor(ContextCompat.getColor(activity, backgroundColor));
        badge.setCornerRadius(12 * activity.getResources().getDisplayMetrics().density);
        view.setBackground(badge);
        view.setTextColor(ContextCompat.getColor(activity, textColor));
    }

    private void progress(int id, int value, int maximum) {
        LinearProgressIndicator indicator = activity.findViewById(id);
        indicator.setMax(maximum);
        indicator.setProgressCompat(value, false);
    }

    private void label(int id, String value) {
        ((TextView) activity.findViewById(id)).setText(value);
    }

    private static int count(List<Interest> interests, String status) {
        int result = 0;
        for (Interest interest : interests) if (status.equals(interest.status)) result++;
        return result;
    }

    private static long[] currentMonth() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
    }

    private static String fallback(String value, String alternative) {
        return value == null || value.isEmpty() ? alternative : value;
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] words = name.trim().split("\\s+");
        return (words[0].substring(0, 1)
                + (words.length > 1 ? words[words.length - 1].substring(0, 1) : ""))
                .toUpperCase(Locale.ROOT);
    }
}

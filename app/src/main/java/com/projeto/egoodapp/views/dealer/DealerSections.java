package com.projeto.egoodapp.views.dealer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.local.AccountProfile;
import com.projeto.egoodapp.data.local.Interest;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.LocalSession;
import com.projeto.egoodapp.data.local.DealerPerformance;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.projeto.egoodapp.views.LocalProfileForms;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class DealerSections {
    private final AppCompatActivity activity;
    private final String owner;
    private final LocalRepository repo;
    private final Runnable openContacts;
    private String filter = "Todos";
    private final String[] statuses = {"Novo", "Em contato", "Finalizado"};
    DealerSections(AppCompatActivity activity, String owner, Runnable openContacts) {
        this.activity = activity; this.owner = owner; repo = LocalRepository.get(activity); this.openContacts = openContacts;
    }
    void dashboard() {
        AccountProfile profile = repo.account(owner);
        String name = fallback(profile.name, "Minha concessionária");
        label(R.id.tvDashboardGreeting, "Bem-vindo(a), " + name + " 👋"); label(R.id.tvDashboardCompany, name);
        List<Interest> interests = repo.interests(owner);
        label(R.id.tvDashboardVehicles, String.valueOf(repo.dealerVehicles(owner).size()));
        label(R.id.tvDashboardInterests, String.valueOf(interests.size()));
        label(R.id.tvDashboardInContact, String.valueOf(interests.stream().filter(i -> "Em contato".equals(i.status)).count()));
        java.util.Calendar calendar = java.util.Calendar.getInstance(); calendar.set(java.util.Calendar.DAY_OF_MONTH, 1);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0); calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0); calendar.set(java.util.Calendar.MILLISECOND, 0);
        long month = calendar.getTimeInMillis();
        DealerPerformance performance = repo.performance(owner, month);
        label(R.id.tvDashboardViews, String.valueOf(performance.views));
        label(R.id.tvDashboardMonthlyContacts, String.valueOf(performance.contacts));
        label(R.id.tvDashboardConversion, performance.conversionPercent + "%");
        progress(R.id.progressDashboardViews, performance.views, DealerPerformance.nextMilestone(performance.views));
        progress(R.id.progressDashboardContacts, performance.contacts, DealerPerformance.nextMilestone(performance.contacts));
        progress(R.id.progressDashboardConversion, performance.conversionPercent, 100);
        LinearLayout list = activity.findViewById(R.id.dashboardRecentInterests); list.removeAllViews();
        for (Interest i : interests.subList(0, Math.min(3, interests.size()))) {
            View card = activity.getLayoutInflater().inflate(R.layout.item_dealer_interest, list, false);
            ((TextView) card.findViewById(R.id.tvInterestAvatar)).setText(initials(i.name));
            ((TextView) card.findViewById(R.id.tvInterestName)).setText(i.name);
            ((TextView) card.findViewById(R.id.tvInterestVehicle)).setText(i.vehicleName);
            ((TextView) card.findViewById(R.id.tvInterestStatus)).setText(i.status);
            card.findViewById(R.id.tvInterestStatus).setOnClickListener(v -> openContacts.run());
            card.findViewById(R.id.tvInterestPhone).setVisibility(View.GONE); card.findViewById(R.id.tvInterestDate).setVisibility(View.GONE);
            ((View) card.findViewById(R.id.btnInterestCall).getParent()).setVisibility(View.GONE);
            card.setOnClickListener(v -> openContacts.run()); list.addView(card);
        }
        if (interests.isEmpty()) {
            TextView empty = new TextView(activity); empty.setText("Nenhum interessado recebido");
            empty.setTextColor(Color.parseColor("#64748B")); list.addView(empty);
        }
    }
    private void progress(int id, int value, int maximum) {
        LinearProgressIndicator indicator = activity.findViewById(id);
        indicator.setMax(maximum); indicator.setProgressCompat(value, false);
    }
    void contacts() {
        List<Interest> contacts = repo.interests(owner);
        label(R.id.tvInterestCount, contacts.size() + (contacts.size() == 1 ? " contato recebido" : " contatos recebidos"));
        int[] ids = {R.id.filterInterest0, R.id.filterInterest1, R.id.filterInterest2, R.id.filterInterest3};
        String[] filters = {"Todos", "Novo", "Em contato", "Finalizado"};
        for (int index = 0; index < ids.length; index++) {
            MaterialButton chip = activity.findViewById(ids[index]); String value = filters[index];
            boolean active = filter.equals(value);
            chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(active ? "#0D9488" : "#F1F3F5")));
            chip.setTextColor(Color.parseColor(active ? "#FFFFFF" : "#64748B"));
            chip.setOnClickListener(v -> { filter = value; contacts(); });
        }
        LinearLayout list = activity.findViewById(R.id.interestsList); list.removeAllViews();
        for (Interest interest : contacts) {
            if (!filter.equals("Todos") && !filter.equals(interest.status)) continue;
            View card = activity.getLayoutInflater().inflate(R.layout.item_dealer_interest, list, false);
            ((TextView) card.findViewById(R.id.tvInterestAvatar)).setText(initials(interest.name));
            ((TextView) card.findViewById(R.id.tvInterestName)).setText(interest.name);
            ((TextView) card.findViewById(R.id.tvInterestVehicle)).setText(interest.vehicleName);
            ((TextView) card.findViewById(R.id.tvInterestPhone)).setText(interest.phone);
            ((TextView) card.findViewById(R.id.tvInterestDate)).setText(new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.forLanguageTag("pt-BR")).format(new Date(interest.createdAt)));
            TextView status = card.findViewById(R.id.tvInterestStatus); status.setText(interest.status);
            status.setContentDescription("Alterar status de " + interest.name);
            String background = "Em contato".equals(interest.status) ? "#DBEAFE" : "Finalizado".equals(interest.status) ? "#F1F3F5" : "#E0F8F7";
            String foreground = "Em contato".equals(interest.status) ? "#1D4ED8" : "Finalizado".equals(interest.status) ? "#475569" : "#0D9488";
            GradientDrawable badge = new GradientDrawable(); badge.setColor(Color.parseColor(background));
            badge.setCornerRadius(12 * activity.getResources().getDisplayMetrics().density); status.setBackground(badge); status.setTextColor(Color.parseColor(foreground));
            status.setOnClickListener(v -> new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_EGood_LocalDialog)
                    .setTitle("Status do contato").setItems(statuses, (dialog, index) -> {
                        repo.updateStatus(owner, interest.id, statuses[index]); contacts();
                    }).setNegativeButton("Cancelar", null).show());
            card.findViewById(R.id.btnInterestCall).setOnClickListener(v -> external(new Intent(Intent.ACTION_DIAL,
                    Uri.parse("tel:" + interest.phone.replaceAll("[^0-9+]", "")))));
            View email = card.findViewById(R.id.btnInterestEmail);
            email.setEnabled(interest.email != null && !interest.email.isEmpty());
            email.setOnClickListener(v -> external(new Intent(Intent.ACTION_SENDTO,
                    Uri.fromParts("mailto", interest.email, null))));
            list.addView(card);
        }
        if (list.getChildCount() == 0) {
            TextView empty = new TextView(activity); empty.setText("Nenhum interessado neste filtro");
            empty.setTextColor(Color.parseColor("#64748B")); empty.setPadding(0, 24, 0, 24); list.addView(empty);
        }
    }
    void profile() {
        AccountProfile p = repo.account(owner);
        label(R.id.tvCompanyName, fallback(p.name, "Minha concessionária"));
        label(R.id.tvCompanyCnpj, "CNPJ: " + fallback(p.cnpj, "Não informado"));
        label(R.id.tvCompanyFieldName, fallback(p.name, "Não informado"));
        label(R.id.tvCompanyPhone, fallback(p.phone, "Não informado"));
        label(R.id.tvCompanyAddress, fallback(p.address, "Não informado"));
        label(R.id.tvCompanyCity, p.city.isEmpty() ? "Não informado" : p.city + ", " + p.state);
        label(R.id.tvCompanyDescription, fallback(p.description, "Nenhuma descrição informada"));
        label(R.id.tvCompanyVehicleCount, String.valueOf(repo.dealerVehicles(owner).size()));
        activity.findViewById(R.id.btnEditCompany).setOnClickListener(v -> LocalProfileForms.edit(activity, p, this::profile));
        activity.findViewById(R.id.btnCompanyLogout).setOnClickListener(v -> LocalSession.logout(activity));
    }
    private void label(int id, String value) { ((TextView) activity.findViewById(id)).setText(value); }
    private static String fallback(String value, String fallback) { return value == null || value.isEmpty() ? fallback : value; }
    private void external(Intent intent) {
        try { activity.startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException e) { Toast.makeText(activity, "Nenhum aplicativo disponível para esta ação", Toast.LENGTH_LONG).show(); }
    }
    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] words = name.trim().split("\\s+");
        return (words[0].substring(0, 1) + (words.length > 1 ? words[words.length - 1].substring(0, 1) : "")).toUpperCase(Locale.ROOT);
    }
}

package com.projeto.egoodapp.views.privacy;

import com.projeto.egoodapp.security.PrivacyPolicy;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.SecurityAudit;
import com.projeto.egoodapp.data.model.SecurityAuditEvent;
import com.projeto.egoodapp.views.common.session.AuthenticatedActivity;
import java.util.HashSet;
import java.util.Set;

/** Shared, scrollable Material dialogs, restored by FragmentManager on rotation. */
public final class PrivacyUi {
    private static final String TAG = "egood_privacy";
    private static final Set<String> prompted = new HashSet<>();
    public interface LocationConsentHost { void onLocationConsentResult(boolean allowed); }
    private PrivacyUi() {}
    public static void resetPrompt() { prompted.clear(); }
    public static AccountProfile current(android.content.Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? null : LocalRepository.get(context).account(user.getUid());
    }
    public static boolean locationAllowed(android.content.Context context) {
        return PrivacyPolicy.locationAllowed(current(context));
    }
    public static void maybeShow(AppCompatActivity activity) {
        try {
            AccountProfile profile = current(activity);
            if (profile == null || PrivacyPolicy.acknowledged(profile)) return;
            String key = profile.uid + ":" + PrivacyPolicy.VERSION;
            if (prompted.contains(key) || activity.getSupportFragmentManager().findFragmentByTag(TAG) != null) return;
            prompted.add(key); show(activity, "ack");
        } catch (RuntimeException ignored) { /* Protected screens handle storage errors through their session guard. */ }
    }
    public static void showPolicy(AppCompatActivity activity) { show(activity, "policy"); }
    public static void showRights(AppCompatActivity activity) { show(activity, "rights"); }
    public static void requestLocation(AppCompatActivity activity) {
        if (activity instanceof AuthenticatedActivity) {
            try {
                AccountProfile profile = current(activity);
                if (PrivacyPolicy.locationDecided(profile)) {
                    deliver(activity, PrivacyPolicy.locationAllowed(profile)); return;
                }
            } catch (RuntimeException failure) { deliver(activity, false); return; }
        }
        show(activity, "location");
    }
    private static void show(AppCompatActivity activity, String mode) {
        if (activity.isFinishing() || activity.getSupportFragmentManager().isStateSaved()) return;
        if (activity.getSupportFragmentManager().findFragmentByTag(TAG) != null) return;
        PrivacyDialog fragment = new PrivacyDialog();
        Bundle args = new Bundle(); args.putString("mode", mode);
        args.putBoolean("draft", !(activity instanceof AuthenticatedActivity));
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        args.putString("uid", user == null ? null : user.getUid());
        fragment.setArguments(args);
        fragment.show(activity.getSupportFragmentManager(), TAG);
    }
    private static void deliver(AppCompatActivity activity, boolean allowed) {
        if (activity instanceof LocationConsentHost) ((LocationConsentHost) activity).onLocationConsentResult(allowed);
    }
    public static final class PrivacyDialog extends DialogFragment {
        private boolean locationAnswered;
        @NonNull @Override public Dialog onCreateDialog(Bundle state) {
            String mode = requireArguments().getString("mode", "policy");
            MaterialAlertDialogBuilder builder = new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(requireContext(), R.style.ThemeOverlay_EGood_LocalDialog);
            if ("location".equals(mode)) {
                builder.setTitle("Uso opcional da localização").setMessage(R.string.location_purpose)
                        .setPositiveButton("Autorizar localização", (dialog, which) -> answerLocation(true))
                        .setNegativeButton("Continuar sem localização", (dialog, which) -> answerLocation(false));
            } else if ("rights".equals(mode)) {
                View content = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_privacy_rights, null);
                ((TextView) content.findViewById(R.id.privacyRightsText)).setText(R.string.lgpd_rights_body);
                SwitchMaterial location = content.findViewById(R.id.switchPrivacyLocation);
                AccountProfile profile = profile();
                location.setChecked(PrivacyPolicy.locationAllowed(profile));
                location.setEnabled(profile != null);
                boolean[] updating = {false};
                location.setOnCheckedChangeListener((button, allowed) -> {
                    if (updating[0]) return;
                    if (allowed) {
                        updating[0] = true; location.setChecked(false); updating[0] = false;
                        // Show a new purpose dialog, preserving the currently visible rights screen.
                        new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(requireContext(), R.style.ThemeOverlay_EGood_LocalDialog)
                                .setTitle("Autorizar localização").setMessage(R.string.location_purpose)
                                .setPositiveButton("Autorizar", (d, w) -> {
                                    if (saveLocation(true)) { location.setOnCheckedChangeListener(null); location.setChecked(true); dismiss(); }
                                }).setNegativeButton("Cancelar", null).show();
                    } else if (!saveLocation(false)) {
                        updating[0] = true; location.setChecked(true); updating[0] = false;
                    }
                });
                content.findViewById(R.id.btnPrivacySystemSettings).setOnClickListener(view -> openSettings());
                content.findViewById(R.id.btnPrivacyRequest).setOnClickListener(view -> requestRights());
                builder.setTitle("LGPD e seus direitos").setView(content).setPositiveButton("Fechar", null);
            } else {
                builder.setTitle("Política de Privacidade").setMessage(getString(R.string.privacy_policy_body, PrivacyPolicy.VERSION));
                if ("ack".equals(mode) || (profile() != null && !PrivacyPolicy.acknowledged(profile()))) builder.setPositiveButton("Li e entendi", (d, w) -> acknowledge())
                        .setNegativeButton("Agora não", null);
                else builder.setPositiveButton("Fechar", null);
            }
            return builder.create();
        }
        private AccountProfile profile() {
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String expected = requireArguments().getString("uid");
                if (user == null || !user.getUid().equals(expected)) return null;
                return LocalRepository.get(requireContext()).account(expected);
            } catch (RuntimeException failure) { return null; }
        }
        private void acknowledge() {
            AccountProfile profile = profile(); if (profile == null) return;
            try {
                PrivacyPolicy.acknowledge(profile, System.currentTimeMillis());
                LocalRepository.get(requireContext()).saveAccount(profile);
                SecurityAudit.record(requireContext(), SecurityAuditEvent.Type.PRIVACY_ACKNOWLEDGED,
                        SecurityAuditEvent.Result.SUCCESS, profile.uid);
            } catch (RuntimeException failure) { error(); }
        }
        private boolean saveLocation(boolean allowed) {
            AccountProfile profile = profile(); if (profile == null) return false;
            boolean previouslyAllowed = PrivacyPolicy.locationAllowed(profile);
            try {
                PrivacyPolicy.location(profile, allowed, System.currentTimeMillis());
                LocalRepository.get(requireContext()).saveAccount(profile);
                SecurityAudit.record(requireContext(), SecurityAuditEvent.Type.LOCATION_PERMISSION,
                        allowed ? SecurityAuditEvent.Result.GRANTED : previouslyAllowed
                        ? SecurityAuditEvent.Result.REVOKED : SecurityAuditEvent.Result.DENIED, profile.uid);
                return true;
            } catch (RuntimeException failure) { error(); return false; }
        }
        private void answerLocation(boolean allowed) {
            locationAnswered = true;
            boolean draft = requireArguments().getBoolean("draft");
            boolean effective = draft ? allowed : saveLocation(allowed) && allowed;
            deliver((AppCompatActivity) requireActivity(), effective);
        }
        @Override public void onCancel(@NonNull DialogInterface dialog) {
            super.onCancel(dialog);
            if ("location".equals(requireArguments().getString("mode")) && !locationAnswered) answerLocation(false);
        }
        private void openSettings() {
            try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + requireContext().getPackageName()))); }
            catch (ActivityNotFoundException failure) { error(); }
        }
        private void requestRights() {
            Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "Solicitação LGPD — e-good")
                    .putExtra(Intent.EXTRA_TEXT, "À equipe do projeto e-good,\n\nDesejo exercer o seguinte direito sobre meus dados: \n\nDescreva seu pedido. Não informe senha ou códigos de acesso.");
            try { startActivity(Intent.createChooser(share, "Compartilhar solicitação com a equipe")); }
            catch (ActivityNotFoundException failure) { error(); }
        }
        private void error() { Toast.makeText(requireContext(), "Não foi possível concluir. Tente novamente.", Toast.LENGTH_LONG).show(); }
    }
}

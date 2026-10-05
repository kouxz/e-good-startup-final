package com.projeto.egoodapp.views.account;

import android.app.Activity;
import android.app.Application;
import android.content.MutableContextWrapper;
import android.os.CancellationSignal;
import androidx.core.content.ContextCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.projeto.egoodapp.data.local.LocalRepository;

/** Owns the sensitive reauthentication and account deletion operation across rotation. */
public final class AccountDeletionViewModel extends AndroidViewModel {
    public enum Phase { IDLE, REAUTHENTICATING, DELETING, SUCCESS, ERROR }

    public static final class State {
        public final Phase phase;
        public final String message;

        private State(Phase phase, String message) {
            this.phase = phase;
            this.message = message;
        }

        public boolean busy() {
            return phase == Phase.REAUTHENTICATING || phase == Phase.DELETING;
        }
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final LocalRepository repository;
    private final MutableLiveData<State> state =
            new MutableLiveData<>(new State(Phase.IDLE, null));
    private MutableContextWrapper googleContext;
    private CancellationSignal googleCancellation;
    private boolean cleared;

    public AccountDeletionViewModel(Application application) {
        super(application);
        repository = LocalRepository.get(application);
    }

    public LiveData<State> state() {
        return state;
    }

    public void attach(Activity activity) {
        if (googleContext != null) googleContext.setBaseContext(activity);
    }

    public void detach(Activity activity) {
        if (googleContext != null && googleContext.getBaseContext() == activity) {
            googleContext.setBaseContext(getApplication());
        }
    }

    public static boolean hasProvider(FirebaseUser user, String providerId) {
        if (user == null) return false;
        for (UserInfo info : user.getProviderData()) {
            if (providerId.equals(info.getProviderId())) return true;
        }
        return false;
    }

    public void deleteWithPassword(String password) {
        if (busy()) return;
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            error("A sessão expirou. Entre novamente antes de excluir a conta.");
            return;
        }
        if (!hasProvider(user, EmailAuthProvider.PROVIDER_ID)) {
            error("Esta conta não possui autenticação por senha.");
            return;
        }
        if (password == null || password.isEmpty()) {
            error("Informe sua senha atual.");
            return;
        }
        beginReauthentication();
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        reauthenticateAndDelete(user, credential);
    }

    public void deleteWithGoogle(Activity activity) {
        if (busy()) return;
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            error("A sessão expirou. Entre novamente antes de excluir a conta.");
            return;
        }
        if (!hasProvider(user, GoogleAuthProvider.PROVIDER_ID)) {
            error("Esta conta não possui autenticação pelo Google.");
            return;
        }
        int clientId = getApplication().getResources().getIdentifier(
                "default_web_client_id", "string", getApplication().getPackageName());
        if (clientId == 0 || getApplication().getString(clientId).trim().isEmpty()) {
            error("A confirmação com Google não está configurada neste aplicativo.");
            return;
        }

        beginReauthentication();
        googleContext = new MutableContextWrapper(activity);
        googleCancellation = new CancellationSignal();
        try {
            GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                    .setServerClientId(getApplication().getString(clientId))
                    .setFilterByAuthorizedAccounts(true)
                    .setAutoSelectEnabled(true)
                    .build();
            GetCredentialRequest request = new GetCredentialRequest.Builder()
                    .addCredentialOption(option).build();
            CredentialManager.create(getApplication()).getCredentialAsync(
                    googleContext, request, googleCancellation,
                    ContextCompat.getMainExecutor(getApplication()),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            releaseGoogleRequest();
                            if (cleared) return;
                            if (!(result.getCredential() instanceof CustomCredential)
                                    || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                    .equals(result.getCredential().getType())) {
                                error("Não foi possível confirmar sua Conta Google.");
                                return;
                            }
                            try {
                                GoogleIdTokenCredential token = GoogleIdTokenCredential.createFrom(
                                        ((CustomCredential) result.getCredential()).getData());
                                reauthenticateAndDelete(user,
                                        GoogleAuthProvider.getCredential(token.getIdToken(), null));
                            } catch (Exception invalidToken) {
                                error("Não foi possível confirmar sua Conta Google.");
                            }
                        }

                        @Override
                        public void onError(GetCredentialException failure) {
                            releaseGoogleRequest();
                            if (cleared) return;
                            if (failure instanceof GetCredentialCancellationException) {
                                error("A confirmação com Google foi cancelada.");
                            } else if (failure instanceof NoCredentialException) {
                                error("Não encontramos uma Conta Google já autorizada para o e-good. "
                                        + "Entre novamente com Google e tente excluir a conta.");
                            } else {
                                error("Não foi possível confirmar sua Conta Google. Verifique sua conexão.");
                            }
                        }
                    });
        } catch (RuntimeException unavailable) {
            releaseGoogleRequest();
            error("A confirmação com Google não está disponível neste aparelho.");
        }
    }

    public void acknowledgeError() {
        State current = state.getValue();
        if (current != null && current.phase == Phase.ERROR) idle();
    }

    public void consumeSuccess() {
        State current = state.getValue();
        if (current != null && current.phase == Phase.SUCCESS) idle();
    }

    private boolean busy() {
        State current = state.getValue();
        return current != null && current.busy();
    }

    private void beginReauthentication() {
        state.setValue(new State(Phase.REAUTHENTICATING, null));
    }

    private void reauthenticateAndDelete(FirebaseUser user, AuthCredential credential) {
        String uid = user.getUid();
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (cleared) return;
            if (!task.isSuccessful()) {
                error(authenticationError(task.getException()));
                return;
            }
            FirebaseUser current = auth.getCurrentUser();
            if (current == null || !uid.equals(current.getUid())) {
                error("A conta confirmada não corresponde à sessão atual.");
                return;
            }
            state.setValue(new State(Phase.DELETING, null));
            current.delete().addOnCompleteListener(deleteTask -> {
                if (cleared) return;
                if (!deleteTask.isSuccessful()) {
                    error("Não foi possível excluir a conta. Tente novamente.");
                    return;
                }
                try {
                    repository.deleteAccount(uid);
                    state.setValue(new State(Phase.SUCCESS, null));
                } catch (RuntimeException storageFailure) {
                    state.setValue(new State(Phase.SUCCESS,
                            "A conta foi excluída, mas alguns dados locais podem exigir a limpeza do aplicativo."));
                }
            });
        });
    }

    private String authenticationError(Exception failure) {
        String code = failure instanceof FirebaseAuthException
                ? ((FirebaseAuthException) failure).getErrorCode() : "";
        if ("ERROR_WRONG_PASSWORD".equals(code)
                || "ERROR_INVALID_CREDENTIAL".equals(code)
                || "ERROR_INVALID_LOGIN_CREDENTIALS".equals(code)) {
            return "A senha ou a conta informada não confere.";
        }
        if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) {
            return "Não foi possível confirmar sua identidade. Verifique sua conexão.";
        }
        return "Não foi possível confirmar sua identidade. Tente novamente.";
    }

    private void error(String message) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) com.projeto.egoodapp.data.local.SecurityAudit.record(getApplication(),
                com.projeto.egoodapp.data.model.SecurityAuditEvent.Type.ACCOUNT_DELETION,
                com.projeto.egoodapp.data.model.SecurityAuditEvent.Result.FAILURE, user.getUid());
        state.setValue(new State(Phase.ERROR, message));
    }

    private void idle() {
        state.setValue(new State(Phase.IDLE, null));
    }

    private void releaseGoogleRequest() {
        googleContext = null;
        googleCancellation = null;
    }

    @Override
    protected void onCleared() {
        cleared = true;
        if (googleCancellation != null) googleCancellation.cancel();
        releaseGoogleRequest();
    }
}

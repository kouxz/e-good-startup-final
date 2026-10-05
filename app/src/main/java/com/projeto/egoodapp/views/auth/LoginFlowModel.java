package com.projeto.egoodapp.views.auth;

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
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.local.AccountAccess;
import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.LocalSession;

/** Keeps an authentication attempt and its account-type confirmation across rotation. */
public final class LoginFlowModel extends AndroidViewModel {
    public enum Phase { IDLE, WORKING, CONFIRM_TYPE, READY, ERROR }
    public static final class State {
        public final Phase phase;
        public final AccountProfile profile;
        public final Exception error;
        public final String message;
        private State(Phase phase, AccountProfile profile, Exception error, String message) {
            this.phase = phase; this.profile = profile; this.error = error; this.message = message;
        }
        public boolean busy() {
            return phase == Phase.WORKING || phase == Phase.CONFIRM_TYPE || phase == Phase.READY;
        }
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final LocalRepository repository;
    private final com.projeto.egoodapp.security.LoginAttemptLimiter limiter;
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Phase.IDLE, null, null, null));
    private String selectedType, pendingUid, candidateType;
    private boolean googleAttempt, cleared;
    private CancellationSignal googleCancellation;
    private MutableContextWrapper googleContext;

    public LoginFlowModel(Application application) {
        super(application);
        repository = LocalRepository.get(application);
        limiter = new com.projeto.egoodapp.security.LoginAttemptLimiter(application);
    }

    public LiveData<State> state() { return state; }
    public String selectedType() { return selectedType; }
    public String candidateType() { return candidateType; }
    public void setCandidateType(String type) {
        if (state.getValue().phase == Phase.CONFIRM_TYPE && AccountAccess.knownType(type)) candidateType = type;
    }
    public boolean googleAttempt() { return googleAttempt; }
    public boolean busy() { return state.getValue().busy(); }

    public void attach(Activity activity) {
        if (googleContext != null) googleContext.setBaseContext(activity);
    }
    public void detach(Activity activity) {
        if (googleContext != null && googleContext.getBaseContext() == activity) {
            googleContext.setBaseContext(getApplication());
        }
    }

    public void passwordLogin(String email, String password, String type) {
        if (busy()) return;
        try {
            if (limiter.remaining() > 0) { error(null, "Aguarde o prazo indicado para tentar novamente."); return; }
        } catch (RuntimeException storageFailure) { error(null, "Não foi possível preparar o acesso. Tente novamente."); return; }
        begin(type, false);
        passwordAttempt(email, password, true);
    }
    private void passwordAttempt(String email, String password, boolean legacyRetry) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful() && !cleared && legacyRetry && !password.equals(password.trim())
                    && !password.trim().isEmpty() && credentialsError(task.getException())) {
                passwordAttempt(email, password.trim(), false);
                return;
            }
            try {
                if (task.isSuccessful()) limiter.success(); else limiter.failure(task.getException());
            } catch (RuntimeException storageFailure) {
                if (task.isSuccessful()) LocalSession.signOut(getApplication(), () -> error(null, "Não foi possível preparar o acesso. Tente novamente."));
                else error(null, "Não foi possível preparar o acesso. Tente novamente.");
                return;
            }
            finishAuthentication(task.isSuccessful() ? task.getResult() : null, task.getException());
        });
    }

    public void googleLogin(Activity activity, String type) {
        if (busy()) return;
        // This generated resource is absent until Google OAuth is configured in Firebase.
        int resource = getApplication().getResources().getIdentifier("default_web_client_id", "string",
                getApplication().getPackageName());
        if (resource == 0 || getApplication().getString(resource).trim().isEmpty()) {
            googleAttempt = true;
            error(null, "O login com Google está indisponível no momento. Use seu e-mail e senha.");
            return;
        }
        begin(type, true);
        googleContext = new MutableContextWrapper(activity);
        googleCancellation = new CancellationSignal();
        try {
            GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(
                    getApplication().getString(resource)).build();
            GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(option).build();
            CredentialManager.create(getApplication()).getCredentialAsync(googleContext, request, googleCancellation,
                    ContextCompat.getMainExecutor(getApplication()),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override public void onResult(GetCredentialResponse result) {
                            releaseGoogleRequest();
                            if (cleared) return;
                            if (!(result.getCredential() instanceof CustomCredential)
                                    || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                    .equals(result.getCredential().getType())) {
                                error(null, "Não foi possível confirmar sua conta Google. Tente novamente.");
                                return;
                            }
                            try {
                                GoogleIdTokenCredential token = GoogleIdTokenCredential.createFrom(
                                        ((CustomCredential) result.getCredential()).getData());
                                auth.signInWithCredential(com.google.firebase.auth.GoogleAuthProvider
                                        .getCredential(token.getIdToken(), null)).addOnCompleteListener(task ->
                                        finishAuthentication(task.isSuccessful() ? task.getResult() : null, task.getException()));
                            } catch (Exception invalidToken) {
                                error(null, "Não foi possível confirmar sua conta Google. Tente novamente.");
                            }
                        }
                        @Override public void onError(GetCredentialException failure) {
                            releaseGoogleRequest();
                            if (cleared) return;
                            if (failure instanceof GetCredentialCancellationException) {
                                idle();
                            } else if (failure instanceof NoCredentialException) {
                                error(null, "Nenhuma conta Google está disponível. Adicione uma conta ao aparelho e tente novamente.");
                            } else {
                                error(null, "Não foi possível conectar com o Google. Verifique sua conexão e tente novamente.");
                            }
                        }
                    });
        } catch (RuntimeException unavailableProvider) {
            releaseGoogleRequest();
            error(null, "O login com Google não está disponível neste aparelho. Use seu e-mail e senha.");
        }
    }

    private void begin(String type, boolean google) {
        if (!AccountAccess.knownType(type)) throw new IllegalArgumentException("Tipo de conta inválido");
        selectedType = type;
        candidateType = type;
        googleAttempt = google;
        pendingUid = null;
        state.setValue(new State(Phase.WORKING, null, null, null));
    }
    private void finishAuthentication(AuthResult result, Exception failure) {
        FirebaseUser user = result == null ? null : result.getUser();
        if (cleared) {
            if (user != null && auth.getCurrentUser() != null
                    && user.getUid().equals(auth.getCurrentUser().getUid())) LocalSession.signOut(getApplication(), () -> {});
            return;
        }
        if (failure != null) { error(failure, null); return; }
        if (user == null) { reject("Não foi possível confirmar a sessão. Tente novamente."); return; }
        try {
            validate(repository.account(user.getUid()), user);
        } catch (RuntimeException storageFailure) {
            reject("Login confirmado, mas não foi possível abrir o perfil local. Tente novamente.");
        }
    }

    private void validate(AccountProfile profile, FirebaseUser user) {
        switch (AccountAccess.evaluate(profile, selectedType)) {
            case ALLOW:
                pendingUid = null;
                limiter.success();
                com.projeto.egoodapp.security.SessionTimeoutController.get(getApplication()).begin(profile.uid);
                com.projeto.egoodapp.data.local.SecurityAudit.record(getApplication(),
                        com.projeto.egoodapp.data.model.SecurityAuditEvent.Type.LOGIN,
                        com.projeto.egoodapp.data.model.SecurityAuditEvent.Result.SUCCESS, profile.uid);
                state.setValue(new State(Phase.READY, profile, null, null));
                break;
            case REJECT:
                reject(getApplication().getString(R.string.login_invalid_type));
                break;
            case CONFIRM_TYPE:
                pendingUid = user.getUid();
                state.setValue(new State(Phase.CONFIRM_TYPE, null, null, null));
                break;
        }
    }

    public void confirmType(String type) {
        if (state.getValue().phase != Phase.CONFIRM_TYPE) return;
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || !user.getUid().equals(pendingUid)) {
            reject("A sessão expirou. Entre novamente.");
            return;
        }
        try {
            AccountProfile profile = repository.confirmAccountType(user, type);
            validate(profile, user);
        } catch (RuntimeException storageFailure) {
            reject("Não foi possível salvar o tipo da conta. Tente novamente.");
        }
    }
    public void cancelTypeConfirmation() {
        if (state.getValue().phase != Phase.CONFIRM_TYPE) return;
        state.setValue(new State(Phase.WORKING, null, null, null));
        pendingUid = null;
        LocalSession.signOut(getApplication(), () -> { if (!cleared) idle(); });
    }
    private void reject(String message) {
        pendingUid = null;
        state.setValue(new State(Phase.WORKING, null, null, null));
        LocalSession.signOut(getApplication(), () -> { if (!cleared) error(null, message); });
    }
    private void error(Exception failure, String message) {
        state.setValue(new State(Phase.ERROR, null, failure, message));
    }
    private void idle() { state.setValue(new State(Phase.IDLE, null, null, null)); }
    public void acknowledge() {
        if (state.getValue().phase == Phase.ERROR || state.getValue().phase == Phase.READY) idle();
    }
    private void releaseGoogleRequest() { googleContext = null; googleCancellation = null; }

    private boolean credentialsError(Exception failure) {
        if (!(failure instanceof FirebaseAuthException)) return false;
        String code = ((FirebaseAuthException) failure).getErrorCode();
        return "ERROR_INVALID_CREDENTIAL".equals(code) || "ERROR_INVALID_LOGIN_CREDENTIALS".equals(code)
                || "ERROR_WRONG_PASSWORD".equals(code) || "ERROR_USER_NOT_FOUND".equals(code);
    }
    @Override protected void onCleared() {
        cleared = true;
        if (googleCancellation != null) googleCancellation.cancel();
        if (state.getValue().phase == Phase.CONFIRM_TYPE) LocalSession.signOut(getApplication(), () -> {});
        releaseGoogleRequest();
    }
}

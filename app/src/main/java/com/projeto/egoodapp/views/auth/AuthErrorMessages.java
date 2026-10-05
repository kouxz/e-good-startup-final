package com.projeto.egoodapp.views.auth;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;

/** Converts authentication failures into user-safe messages without exposing server details. */
public final class AuthErrorMessages {
    private AuthErrorMessages() {}

    public static boolean invalidCredentials(Exception failure) {
        return invalidCredentialsCode(code(failure));
    }
    public static boolean invalidCredentialsCode(String code) {
        return "ERROR_INVALID_CREDENTIAL".equals(code) || "ERROR_INVALID_LOGIN_CREDENTIALS".equals(code)
                || "ERROR_WRONG_PASSWORD".equals(code) || "ERROR_USER_NOT_FOUND".equals(code);
    }
    public static boolean tooManyRequests(Exception failure) {
        return failure instanceof FirebaseTooManyRequestsException || "ERROR_TOO_MANY_REQUESTS".equals(code(failure));
    }
    private static String code(Exception failure) {
        return failure instanceof FirebaseAuthException ? ((FirebaseAuthException) failure).getErrorCode() : "";
    }
    public static String login(Exception failure, boolean google) {
        if (failure instanceof FirebaseNetworkException) return "Verifique sua internet e tente novamente.";
        if (tooManyRequests(failure)) return "Muitas tentativas seguidas. Aguarde e tente novamente.";
        return loginCode(code(failure), google);
    }
    static String loginCode(String code, boolean google) {
        if (invalidCredentialsCode(code)) return google
                ? "Não foi possível confirmar sua conta Google. Tente novamente."
                : "E-mail ou senha não conferem. Confira os dados ou recupere sua senha.";
        switch (code) {
            case "ERROR_INVALID_EMAIL": return "Informe um e-mail em formato válido.";
            case "ERROR_USER_DISABLED": return "Esta conta está indisponível. Entre em contato com a equipe.";
            case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
            case "ERROR_CREDENTIAL_ALREADY_IN_USE": return "Use o método com que sua conta foi cadastrada.";
            case "ERROR_OPERATION_NOT_ALLOWED":
            case "ERROR_PASSWORD_LOGIN_DISABLED": return "Este método de acesso está indisponível no momento.";
            case "ERROR_NETWORK_REQUEST_FAILED": return "Verifique sua internet e tente novamente.";
            case "ERROR_TOO_MANY_REQUESTS": return "Muitas tentativas seguidas. Aguarde e tente novamente.";
            default: return "Não foi possível concluir a solicitação. Tente novamente.";
        }
    }
    public static String recovery(Exception failure) {
        if (invalidCredentials(failure) || failure == null) return "Se esse e-mail estiver cadastrado, você receberá as instruções de redefinição.";
        return login(failure, false);
    }

    public static String registration(Exception failure) {
        if (failure instanceof FirebaseNetworkException) {
            return "Sem conexão com o Firebase. Verifique sua internet e tente novamente.";
        }
        if (failure instanceof FirebaseTooManyRequestsException) {
            return "Muitas tentativas seguidas. Aguarde um pouco e tente novamente.";
        }
        String code = failure instanceof FirebaseAuthException
                ? ((FirebaseAuthException) failure).getErrorCode() : "";
        return registrationCode(code);
    }

    static String registrationCode(String code) {
        switch (code) {
            case "ERROR_EMAIL_ALREADY_IN_USE":
                return "Não foi possível cadastrar esse e-mail. Tente entrar ou recuperar a senha.";
            case "ERROR_INVALID_EMAIL":
                return "Informe um e-mail em formato válido.";
            case "ERROR_WEAK_PASSWORD":
                return "Use de 8 a 30 caracteres, com maiúscula, minúscula, número e símbolo.";
            case "ERROR_OPERATION_NOT_ALLOWED":
            case "ERROR_PASSWORD_LOGIN_DISABLED":
                return "O cadastro por e-mail está indisponível no momento.";
            case "ERROR_NETWORK_REQUEST_FAILED":
                return "Sem conexão com o Firebase. Verifique sua internet e tente novamente.";
            case "ERROR_TOO_MANY_REQUESTS":
                return "Muitas tentativas seguidas. Aguarde um pouco e tente novamente.";
            default:
                return "Não foi possível concluir o cadastro. Tente novamente.";
        }
    }
}

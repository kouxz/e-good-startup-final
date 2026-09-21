package com.projeto.egoodapp.data.local;

/** The account type belongs to the saved UID, never to the selected login tab. */
public final class AccountAccess {
    public enum Decision { ALLOW, REJECT, CONFIRM_TYPE }
    private AccountAccess() {}

    public static boolean knownType(String type) {
        return "pessoa".equals(type) || "concessionaria".equals(type);
    }

    public static Decision evaluate(AccountProfile profile, String selectedType) {
        if (!knownType(selectedType)) throw new IllegalArgumentException("Tipo de conta inválido");
        if (profile == null || !knownType(profile.type)) return Decision.CONFIRM_TYPE;
        return selectedType.equals(profile.type) ? Decision.ALLOW : Decision.REJECT;
    }
}

package com.projeto.egoodapp.security;

/** Mirrors the password policy to configure in Firebase; never hashes or saves a password. */
public final class PasswordPolicy {
    public static final int MIN_LENGTH = 8, MAX_LENGTH = 30;
    private static final String SPECIAL = "^$*.[]{}()?\"!@#%&/\\,><':;|_~";
    private PasswordPolicy() {}

    public static boolean[] rules(String value) {
        String password = value == null ? "" : value;
        boolean lower = false, upper = false, digit = false, special = false;
        for (char c : password.toCharArray()) {
            lower |= c >= 'a' && c <= 'z'; upper |= c >= 'A' && c <= 'Z';
            digit |= c >= '0' && c <= '9'; special |= SPECIAL.indexOf(c) >= 0;
        }
        return new boolean[]{password.length() >= MIN_LENGTH && password.length() <= MAX_LENGTH,
                upper, lower, digit, special};
    }
    public static boolean valid(String value) {
        for (boolean met : rules(value)) if (!met) return false;
        return true;
    }
}

package com.projeto.egoodapp.security;

/** Bounds text without verifying the existence of academic test emails or companies. */
public final class InputRules {
    public static final int NAME = 120, EMAIL = 254, PHONE = 11, CNPJ = 14,
            ADDRESS = 240, CITY = 100, STATE = 2, ZIP = 8, DESCRIPTION = 2000,
            VEHICLE_TEXT = 100, CHARGING = 80;
    private InputRules() {}

    public static String clean(String value, int maximum) {
        if (value == null) return "";
        StringBuilder text = new StringBuilder();
        value.codePoints().filter(c -> !Character.isISOControl(c)
                && Character.getType(c) != Character.FORMAT).forEach(text::appendCodePoint);
        String normalized = text.toString().trim();
        if (normalized.length() > maximum) throw new IllegalArgumentException("O texto excede o limite permitido");
        return normalized;
    }
}

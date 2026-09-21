package com.projeto.egoodapp.data.local;

public class AccountProfile {
    public String uid, type, name = "", email = "", phone = "";
    public String cnpj = "", address = "", city = "", state = "", zip = "", description = "";
    public Double latitude, longitude;
    public Boolean notificationsEnabled, newInterestsEnabled;

    public boolean isDealer() { return "concessionaria".equals(type); }
    public boolean notificationsEnabled() { return notificationsEnabled == null || notificationsEnabled; }
    public boolean newInterestsEnabled() { return newInterestsEnabled == null || newInterestsEnabled; }
    public boolean hasContact() { return !name.trim().isEmpty() && validPhone(phone); }
    public boolean hasCompanyData() {
        return hasContact() && !cnpj.trim().isEmpty() && !address.trim().isEmpty()
                && !city.trim().isEmpty() && !state.trim().isEmpty();
    }
    public static boolean validPhone(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        return digits.length() >= 10 && digits.length() <= 13;
    }
}

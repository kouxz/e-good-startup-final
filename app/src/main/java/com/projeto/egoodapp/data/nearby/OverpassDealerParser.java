package com.projeto.egoodapp.data.nearby;

import com.projeto.egoodapp.data.model.Concessionaria;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public final class OverpassDealerParser {
    private static final double EARTH_RADIUS_KM = 6371.0088d;

    private OverpassDealerParser() {
    }

    public static List<Concessionaria> parse(JSONArray elements, double userLatitude,
            double userLongitude) {
        List<Concessionaria> found = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (int index = 0; index < elements.length(); index++) {
            JSONObject element = elements.optJSONObject(index);
            if (element == null) continue;

            long osmId = element.optLong("id", -1L);
            if (osmId < 0L) continue;
            String type = normalizedType(element.optString("type", "node"));
            String stableKey = "osm:" + type + ":" + osmId;

            double latitude = element.optDouble("lat", Double.NaN);
            double longitude = element.optDouble("lon", Double.NaN);
            if (!NearbyLocationPolicy.hasValidCoordinates(latitude, longitude)) {
                JSONObject center = element.optJSONObject("center");
                if (center != null) {
                    latitude = center.optDouble("lat", Double.NaN);
                    longitude = center.optDouble("lon", Double.NaN);
                }
            }
            if (!NearbyLocationPolicy.hasValidCoordinates(latitude, longitude)) continue;
            if (!keys.add(stableKey)) continue;

            JSONObject tags = element.optJSONObject("tags");
            String name = tags == null ? "" : tags.optString("name", "").trim();
            if (name.isEmpty()) name = "Concessionária";
            String street = tags == null ? "" : tags.optString("addr:street", "");
            String number = tags == null ? "" : tags.optString("addr:housenumber", "");
            String city = tags == null ? "" : tags.optString("addr:city", "");
            String phone = tags == null ? "" : firstNonEmpty(
                    tags.optString("phone", ""),
                    tags.optString("contact:phone", ""),
                    tags.optString("phone:work", ""));
            double distance = distanceKm(userLatitude, userLongitude, latitude, longitude);

            Concessionaria dealership = new Concessionaria(
                    name, buildAddress(street, number, city), distance, latitude, longitude);
            dealership.setStableKey(stableKey);
            dealership.setTelefone(phone);
            found.add(dealership);
        }
        return found;
    }

    private static String normalizedType(String value) {
        if ("way".equals(value) || "relation".equals(value)) return value;
        return "node";
    }

    private static String buildAddress(String street, String number, String city) {
        String address = street.trim();
        if (!number.trim().isEmpty()) {
            address += (address.isEmpty() ? "" : ", ") + number.trim();
        }
        if (!city.trim().isEmpty()) {
            address += (address.isEmpty() ? "" : " — ") + city.trim();
        }
        return address.isEmpty() ? "Endereço não cadastrado" : address;
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private static double distanceKm(double fromLatitude, double fromLongitude,
            double toLatitude, double toLongitude) {
        double latitudeDelta = Math.toRadians(toLatitude - fromLatitude);
        double longitudeDelta = Math.toRadians(toLongitude - fromLongitude);
        double startLatitude = Math.toRadians(fromLatitude);
        double endLatitude = Math.toRadians(toLatitude);
        double a = Math.sin(latitudeDelta / 2d) * Math.sin(latitudeDelta / 2d)
                + Math.cos(startLatitude) * Math.cos(endLatitude)
                * Math.sin(longitudeDelta / 2d) * Math.sin(longitudeDelta / 2d);
        return EARTH_RADIUS_KM * 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
    }
}

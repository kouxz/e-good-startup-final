package com.projeto.egoodapp.data.model;

import java.util.ArrayList;
import java.util.List;

public class Concessionaria {
    private String nome;
    private String endereco;
    private double distanciaKm;
    private double lat;
    private double lon;
    private String telefone; // Novo campo para telefone
    private String dealerId;
    private String stableKey;
    private boolean hasLocation = true;
    private double ratingAverage;
    private int ratingCount;
    private Integer userRating;
    private int vehicleCount = -1;
    private final List<String> brands = new ArrayList<>();
    public String getDealerId() { return dealerId; }
    public void setDealerId(String value) { dealerId = value; }
    public String getStableKey() { return stableKey; }
    public void setStableKey(String value) { stableKey = value; }
    public boolean hasLocation() { return hasLocation; }
    public void setHasLocation(boolean value) { hasLocation = value; }
    public double getRatingAverage() { return ratingAverage; }
    public int getRatingCount() { return ratingCount; }
    public Integer getUserRating() { return userRating; }
    public void setRating(double average, int count, Integer current) {
        ratingAverage = average; ratingCount = count; userRating = current;
    }
    public int getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(int value) { vehicleCount = value; }
    public List<String> getBrands() { return brands; }
    public void setBrands(List<String> values) { brands.clear(); if (values != null) brands.addAll(values); }
    public boolean isLocal() { return dealerId != null && !dealerId.isEmpty(); }

    public Concessionaria(String nome, String endereco, double distanciaKm, double lat, double lon) {
        this.nome = nome;
        this.endereco = endereco;
        this.distanciaKm = distanciaKm;
        this.lat = lat;
        this.lon = lon;
        this.telefone = ""; // Padrão: vazio
    }

    // Getters e Setters
    public String getNome() { return nome; }
    public String getEndereco() { return endereco; }
    public double getDistanciaKm() { return distanciaKm; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone != null ? telefone : ""; }
}


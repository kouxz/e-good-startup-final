package com.projeto.egoodapp.views.dealership;

public class Concessionaria {
    private String nome;
    private String endereco;
    private double distanciaKm;
    private boolean curtida; // Sistema de like
    private double lat;
    private double lon;
    private String telefone; // Novo campo para telefone
    private String dealerId;
    private boolean hasLocation = true;
    public String getDealerId() { return dealerId; }
    public void setDealerId(String value) { dealerId = value; }
    public boolean hasLocation() { return hasLocation; }
    public void setHasLocation(boolean value) { hasLocation = value; }

    public Concessionaria(String nome, String endereco, double distanciaKm, double lat, double lon) {
        this.nome = nome;
        this.endereco = endereco;
        this.distanciaKm = distanciaKm;
        this.lat = lat;
        this.lon = lon;
        this.curtida = false; // Padrão: não curtida
        this.telefone = ""; // Padrão: vazio
    }

    // Getters e Setters
    public String getNome() { return nome; }
    public String getEndereco() { return endereco; }
    public double getDistanciaKm() { return distanciaKm; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public boolean isCurtida() { return curtida; }
    public void setCurtida(boolean curtida) { this.curtida = curtida; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone != null ? telefone : ""; }
}


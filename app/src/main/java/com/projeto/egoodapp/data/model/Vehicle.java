package com.projeto.egoodapp.data.model;

import java.io.Serializable;

public class Vehicle implements Serializable {
    private String id;
    private String marca;
    private String modelo;
    private int ano;
    private int quilometragem;
    private double preco;
    private String categoria;
    private int bateria;
    private int autonomia;
    private String cor;
    private String descricao;
    private String imagemUrl;
    private String concessionariaId;
    private Double consumo;
    private Integer potencia;
    private String carga;
    private String badge;
    private String imageName;

    public Vehicle() {}

    public Vehicle(String marca, String modelo, int ano, int quilometragem,
                   double preco, String categoria, int bateria, int autonomia,
                   String cor, String descricao) {
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.quilometragem = quilometragem;
        this.preco = preco;
        this.categoria = categoria;
        this.bateria = bateria;
        this.autonomia = autonomia;
        this.cor = cor;
        this.descricao = descricao;
        this.id = java.util.UUID.randomUUID().toString();
    }

    public Vehicle(String marca, String modelo, int ano, int quilometragem,
                   double preco, String categoria, int bateria, int autonomia,
                   String cor, String descricao, String imagemUrl) {
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.quilometragem = quilometragem;
        this.preco = preco;
        this.categoria = categoria;
        this.bateria = bateria;
        this.autonomia = autonomia;
        this.cor = cor;
        this.descricao = descricao;
        this.imagemUrl = imagemUrl;
        this.id = java.util.UUID.randomUUID().toString();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public int getQuilometragem() { return quilometragem; }
    public void setQuilometragem(int quilometragem) { this.quilometragem = quilometragem; }

    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public int getBateria() { return bateria; }
    public void setBateria(int bateria) { this.bateria = bateria; }

    public int getAutonomia() { return autonomia; }
    public void setAutonomia(int autonomia) { this.autonomia = autonomia; }

    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getImagemUrl() { return imagemUrl; }
    public void setImagemUrl(String imagemUrl) { this.imagemUrl = imagemUrl; }
    public String getConcessionariaId() { return concessionariaId; }
    public void setConcessionariaId(String id) { concessionariaId = id; }
    public Double getConsumo() { return consumo; }
    public void setConsumo(Double value) { consumo = value; }
    public Integer getPotencia() { return potencia; }
    public void setPotencia(Integer value) { potencia = value; }
    public String getCarga() { return carga; }
    public void setCarga(String value) { carga = value; }
    public String getBadge() { return badge; }
    public void setBadge(String value) { badge = value; }
    public String getImageName() { return imageName; }
    public void setImageName(String value) { imageName = value; }
    public String getNome() { return ((marca == null ? "" : marca) + " " + (modelo == null ? "" : modelo)).trim(); }
}

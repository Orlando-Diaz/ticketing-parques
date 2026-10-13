package com.orlandodiaz.ticketing.precios.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "regla_precio")
public class ReglaPrecioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posicion", nullable = false)
    private int posicion;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "porcentaje")
    private Integer porcentaje;

    @Column(name = "tamano_minimo")
    private Integer tamanoMinimo;

    protected ReglaPrecioJpaEntity() {
    }

    public ReglaPrecioJpaEntity(int posicion, String tipo, Integer porcentaje, Integer tamanoMinimo) {
        this.posicion = posicion;
        this.tipo = tipo;
        this.porcentaje = porcentaje;
        this.tamanoMinimo = tamanoMinimo;
    }

    public String getTipo() {
        return tipo;
    }

    public Integer getPorcentaje() {
        return porcentaje;
    }

    public Integer getTamanoMinimo() {
        return tamanoMinimo;
    }
}

package com.orlandodiaz.ticketing.precios.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoria_tarifa")
public class CategoriaTarifaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posicion", nullable = false)
    private int posicion;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "edad_minima", nullable = false)
    private int edadMinima;

    @Column(name = "edad_maxima")
    private Integer edadMaxima;

    @Column(name = "precio", nullable = false)
    private long precio;

    protected CategoriaTarifaJpaEntity() {
    }

    public CategoriaTarifaJpaEntity(int posicion, String nombre, int edadMinima, Integer edadMaxima, long precio) {
        this.posicion = posicion;
        this.nombre = nombre;
        this.edadMinima = edadMinima;
        this.edadMaxima = edadMaxima;
        this.precio = precio;
    }

    public String getNombre() {
        return nombre;
    }

    public int getEdadMinima() {
        return edadMinima;
    }

    public Integer getEdadMaxima() {
        return edadMaxima;
    }

    public long getPrecio() {
        return precio;
    }
}

package com.orlandodiaz.ticketing.ventas.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "linea_orden")
public class LineaOrdenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posicion", nullable = false)
    private int posicion;

    @Column(name = "edad_visitante", nullable = false)
    private int edadVisitante;

    @Column(name = "residente", nullable = false)
    private boolean residente;

    @Column(name = "precio", nullable = false)
    private long precio;

    protected LineaOrdenJpaEntity() {
    }

    public LineaOrdenJpaEntity(int posicion, int edadVisitante, boolean residente, long precio) {
        this.posicion = posicion;
        this.edadVisitante = edadVisitante;
        this.residente = residente;
        this.precio = precio;
    }

    public int getEdadVisitante() {
        return edadVisitante;
    }

    public boolean isResidente() {
        return residente;
    }

    public long getPrecio() {
        return precio;
    }
}

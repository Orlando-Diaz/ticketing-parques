package com.orlandodiaz.ticketing.catalogo.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "disponibilidad_diaria")
public class DisponibilidadJpaEntity {

    @EmbeddedId
    private DisponibilidadClave clave;

    @Column(name = "cupo_maximo", nullable = false)
    private int cupoMaximo;

    @Column(name = "vendidas", nullable = false)
    private int vendidas;

    protected DisponibilidadJpaEntity() {
    }

    public DisponibilidadJpaEntity(DisponibilidadClave clave, int cupoMaximo, int vendidas) {
        this.clave = clave;
        this.cupoMaximo = cupoMaximo;
        this.vendidas = vendidas;
    }

    public DisponibilidadClave getClave() {
        return clave;
    }

    public int getCupoMaximo() {
        return cupoMaximo;
    }

    public int getVendidas() {
        return vendidas;
    }
}

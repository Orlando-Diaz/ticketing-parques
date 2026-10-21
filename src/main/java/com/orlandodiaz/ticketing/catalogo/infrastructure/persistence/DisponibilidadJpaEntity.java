package com.orlandodiaz.ticketing.catalogo.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "disponibilidad_diaria")
public class DisponibilidadJpaEntity {

    @EmbeddedId
    private DisponibilidadClave clave;

    @Column(name = "cupo_maximo", nullable = false)
    private int cupoMaximo;

    @Column(name = "vendidas", nullable = false)
    private int vendidas;

    /**
     * Bloqueo optimista: Hibernate lo sube solo en cada UPDATE y agrega "WHERE version = ?".
     * Es un Long (no long) a proposito: null significa "fila nueva, todavia no guardada".
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected DisponibilidadJpaEntity() {
    }

    /** @param version la que se leyo de la base, o null si es una fila nueva */
    public DisponibilidadJpaEntity(DisponibilidadClave clave, int cupoMaximo, int vendidas, Long version) {
        this.clave = clave;
        this.cupoMaximo = cupoMaximo;
        this.vendidas = vendidas;
        this.version = version;
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

    public Long getVersion() {
        return version;
    }
}

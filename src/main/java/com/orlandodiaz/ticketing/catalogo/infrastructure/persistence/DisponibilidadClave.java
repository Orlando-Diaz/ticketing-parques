package com.orlandodiaz.ticketing.catalogo.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/** Llave primaria compuesta: un registro por parque y por fecha. */
@Embeddable
public class DisponibilidadClave implements Serializable {

    @Column(name = "parque_id")
    private String parqueId;

    @Column(name = "fecha")
    private LocalDate fecha;

    protected DisponibilidadClave() {
    }

    public DisponibilidadClave(String parqueId, LocalDate fecha) {
        this.parqueId = parqueId;
        this.fecha = fecha;
    }

    public String getParqueId() {
        return parqueId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DisponibilidadClave otra
                && Objects.equals(parqueId, otra.parqueId) && Objects.equals(fecha, otra.fecha);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parqueId, fecha);
    }
}

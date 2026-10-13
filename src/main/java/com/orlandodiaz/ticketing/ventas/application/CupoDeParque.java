package com.orlandodiaz.ticketing.ventas.application;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;

/** PUERTO: lo que ventas necesita del cupo. Un adaptador lo conecta con el modulo catalogo. */
public interface CupoDeParque {
    void reservar(ParqueId parqueId, LocalDate fecha, int cantidad);

    void liberar(ParqueId parqueId, LocalDate fecha, int cantidad);
}

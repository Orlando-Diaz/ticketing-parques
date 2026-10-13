package com.orlandodiaz.ticketing.catalogo.application;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;

/** CASO DE USO: cuantas entradas quedan para un parque en una fecha (solo consulta, no guarda nada). */
public class ConsultarCupoDisponible {

    private final RepositorioDeParques parques;
    private final RepositorioDeDisponibilidad disponibilidades;

    public ConsultarCupoDisponible(RepositorioDeParques parques, RepositorioDeDisponibilidad disponibilidades) {
        this.parques = parques;
        this.disponibilidades = disponibilidades;
    }

    public int ejecutar(ParqueId parqueId, LocalDate fecha) {
        var parque = parques.buscarPorId(parqueId)
                .orElseThrow(() -> new ParqueNoEncontradoException(parqueId));
        return disponibilidades.buscar(parqueId, fecha)
                .orElseGet(() -> parque.crearDisponibilidad(fecha))
                .disponibles();
    }
}

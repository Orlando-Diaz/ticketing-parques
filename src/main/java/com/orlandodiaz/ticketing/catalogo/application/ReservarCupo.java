package com.orlandodiaz.ticketing.catalogo.application;

import com.orlandodiaz.ticketing.catalogo.domain.DisponibilidadDiaria;
import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;

/**
 * CASO DE USO: reservar cupo para una fecha.
 * Si todavia no existe la disponibilidad de ese dia, la crea con el cupo por defecto del parque.
 * La regla "no pasarse del cupo" NO esta aqui: la hace DisponibilidadDiaria.reservar().
 *
 * OJO (se arregla en la fase de concurrencia): este flujo es leer -> modificar -> guardar.
 * Si dos compradores entran a la vez, ambos pueden leer el mismo cupo y pasarse. Lo vamos a ver fallar.
 */
public class ReservarCupo {

    private final RepositorioDeParques parques;
    private final RepositorioDeDisponibilidad disponibilidades;

    public ReservarCupo(RepositorioDeParques parques, RepositorioDeDisponibilidad disponibilidades) {
        this.parques = parques;
        this.disponibilidades = disponibilidades;
    }

    public void ejecutar(ParqueId parqueId, LocalDate fecha, int cantidad) {
        Parque parque = parques.buscarPorId(parqueId)
                .orElseThrow(() -> new ParqueNoEncontradoException(parqueId));
        DisponibilidadDiaria disponibilidad = disponibilidades.buscar(parqueId, fecha)
                .orElseGet(() -> parque.crearDisponibilidad(fecha));

        disponibilidad.reservar(cantidad);
        disponibilidades.guardar(disponibilidad);
    }
}

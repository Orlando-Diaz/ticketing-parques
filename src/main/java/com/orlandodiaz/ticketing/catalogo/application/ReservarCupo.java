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
 * CONCURRENCIA: el flujo es leer -> modificar -> guardar. Si dos compradores entran a la vez, ambos leen el
 * mismo cupo; el repositorio deja guardar solo al primero y al segundo le lanza
 * ConflictoDeConcurrenciaException. Aqui se reintenta TODO el flujo (volviendo a leer), asi el segundo
 * comprador ve el cupo ya actualizado y, si no alcanza, recibe CupoAgotadoException como corresponde.
 */
public class ReservarCupo {

    private final RepositorioDeParques parques;
    private final RepositorioDeDisponibilidad disponibilidades;
    private final int maximoDeIntentos;

    public ReservarCupo(RepositorioDeParques parques, RepositorioDeDisponibilidad disponibilidades) {
        this(parques, disponibilidades, ReintentoPorConflicto.INTENTOS_POR_DEFECTO);
    }

    public ReservarCupo(RepositorioDeParques parques, RepositorioDeDisponibilidad disponibilidades,
                        int maximoDeIntentos) {
        if (maximoDeIntentos < 1) {
            throw new IllegalArgumentException("Se necesita al menos 1 intento");
        }
        this.parques = parques;
        this.disponibilidades = disponibilidades;
        this.maximoDeIntentos = maximoDeIntentos;
    }

    public void ejecutar(ParqueId parqueId, LocalDate fecha, int cantidad) {
        Parque parque = parques.buscarPorId(parqueId)
                .orElseThrow(() -> new ParqueNoEncontradoException(parqueId));

        ReintentoPorConflicto.ejecutar(maximoDeIntentos, () -> {
            DisponibilidadDiaria disponibilidad = disponibilidades.buscar(parqueId, fecha)
                    .orElseGet(() -> parque.crearDisponibilidad(fecha));
            disponibilidad.reservar(cantidad);
            disponibilidades.guardar(disponibilidad);
        });
    }
}

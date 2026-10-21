package com.orlandodiaz.ticketing.catalogo.infrastructure;

import com.orlandodiaz.ticketing.catalogo.application.ConflictoDeConcurrenciaException;
import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeDisponibilidad;
import com.orlandodiaz.ticketing.catalogo.domain.DisponibilidadDiaria;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador en memoria (desarrollo y pruebas). Respeta el mismo contrato de concurrencia que el de base de datos:
 * guarda COPIAS (no el mismo objeto que usa quien lo leyo) y rechaza guardar si la version ya cambio.
 */
public class RepositorioEnMemoriaDeDisponibilidad implements RepositorioDeDisponibilidad {

    private record Clave(ParqueId parqueId, LocalDate fecha) {
    }

    private record Fila(int cupoMaximo, int vendidas, long version) {
    }

    private final Map<Clave, Fila> almacen = new ConcurrentHashMap<>();

    @Override
    public Optional<DisponibilidadDiaria> buscar(ParqueId parqueId, LocalDate fecha) {
        return Optional.ofNullable(almacen.get(new Clave(parqueId, fecha)))
                .map(f -> DisponibilidadDiaria.reconstruir(parqueId, fecha, f.cupoMaximo(), f.vendidas(), f.version()));
    }

    @Override
    public void guardar(DisponibilidadDiaria d) {
        // compute() es atomico por clave: la comparacion de versiones y el guardado no se pueden intercalar
        almacen.compute(new Clave(d.parqueId(), d.fecha()), (clave, actual) -> {
            if (actual == null) {
                if (d.version() != null) {
                    throw new ConflictoDeConcurrenciaException("El cupo ya no existe");
                }
                return new Fila(d.cupoMaximo(), d.vendidas(), 0);
            }
            if (d.version() == null || d.version() != actual.version()) {
                throw new ConflictoDeConcurrenciaException("Otro comprador modifico este cupo antes");
            }
            return new Fila(d.cupoMaximo(), d.vendidas(), actual.version() + 1);
        });
    }
}

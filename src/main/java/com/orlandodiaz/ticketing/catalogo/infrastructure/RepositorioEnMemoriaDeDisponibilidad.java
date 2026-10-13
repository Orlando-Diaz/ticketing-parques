package com.orlandodiaz.ticketing.catalogo.infrastructure;

import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeDisponibilidad;
import com.orlandodiaz.ticketing.catalogo.domain.DisponibilidadDiaria;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Adaptador en memoria (desarrollo y pruebas). */
public class RepositorioEnMemoriaDeDisponibilidad implements RepositorioDeDisponibilidad {

    private record Clave(ParqueId parqueId, LocalDate fecha) {
    }

    private final Map<Clave, DisponibilidadDiaria> almacen = new ConcurrentHashMap<>();

    @Override
    public Optional<DisponibilidadDiaria> buscar(ParqueId parqueId, LocalDate fecha) {
        return Optional.ofNullable(almacen.get(new Clave(parqueId, fecha)));
    }

    @Override
    public void guardar(DisponibilidadDiaria disponibilidad) {
        almacen.put(new Clave(disponibilidad.parqueId(), disponibilidad.fecha()), disponibilidad);
    }
}

package com.orlandodiaz.ticketing.catalogo.application;

import com.orlandodiaz.ticketing.catalogo.domain.DisponibilidadDiaria;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.Optional;

/** Puerto de salida para guardar y recuperar el cupo de cada parque por fecha. */
public interface RepositorioDeDisponibilidad {

    Optional<DisponibilidadDiaria> buscar(ParqueId parqueId, LocalDate fecha);

    void guardar(DisponibilidadDiaria disponibilidad);
}

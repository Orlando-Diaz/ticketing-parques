package com.orlandodiaz.ticketing.catalogo.application;

import com.orlandodiaz.ticketing.catalogo.domain.DisponibilidadDiaria;
import com.orlandodiaz.ticketing.shared.application.ConflictoDeConcurrenciaException;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Puerto de salida para guardar y recuperar el cupo de cada parque por fecha.
 *
 * CONTRATO DE CONCURRENCIA: guardar() solo debe tener exito si nadie guardo ese mismo cupo desde que se leyo
 * (la disponibilidad trae su version). Si alguien se adelanto, debe lanzar ConflictoDeConcurrenciaException
 * SIN modificar nada. Tambien aplica a la primera vez: si dos crean el mismo dia a la vez, solo uno gana.
 */
public interface RepositorioDeDisponibilidad {

    Optional<DisponibilidadDiaria> buscar(ParqueId parqueId, LocalDate fecha);

    /** @throws ConflictoDeConcurrenciaException si otro guardo esta misma disponibilidad antes. */
    void guardar(DisponibilidadDiaria disponibilidad);
}

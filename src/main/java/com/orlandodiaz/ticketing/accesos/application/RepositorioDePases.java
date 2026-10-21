package com.orlandodiaz.ticketing.accesos.application;

import com.orlandodiaz.ticketing.accesos.domain.Pase;
import com.orlandodiaz.ticketing.shared.application.ConflictoDeConcurrenciaException;
import java.util.Optional;

/**
 * Puerto de salida para guardar y recuperar pases.
 *
 * CONTRATO DE CONCURRENCIA: guardar() solo tiene exito si nadie guardo ese mismo pase desde que se leyo
 * (el pase trae su version). Tambien rechaza crear un pase con un codigo u orden que ya existen.
 */
public interface RepositorioDePases {

    Optional<Pase> buscarPorCodigo(String codigo);

    Optional<Pase> buscarPorOrden(String ordenRef);

    /** @throws ConflictoDeConcurrenciaException si otro guardo este pase antes (o ya existe el codigo/orden). */
    void guardar(Pase pase);
}

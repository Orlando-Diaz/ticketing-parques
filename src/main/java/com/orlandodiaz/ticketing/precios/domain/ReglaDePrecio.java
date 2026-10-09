package com.orlandodiaz.ticketing.precios.domain;

import java.util.Optional;

/**
 * Patron Strategy. Cada regla de negocio es una clase que implementa esta interfaz.
 * Si la regla no aplica devuelve Optional.empty().
 * Agregar una regla nueva = crear una clase nueva, sin tocar las demas (principio O).
 */
public interface ReglaDePrecio {
    Optional<Ajuste> evaluar(ContextoDePrecio contexto);
}

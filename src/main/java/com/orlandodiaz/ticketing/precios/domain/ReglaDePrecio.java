package com.orlandodiaz.ticketing.precios.domain;

import java.util.Optional;

/**
 * Patron Strategy. Cada regla de negocio es una clase que implementa esta interfaz.
 * Si la regla no aplica devuelve Optional.empty().
 * Agregar una regla nueva = crear una clase nueva (y registrarla en FabricaDeReglas), sin tocar las demas.
 */
public interface ReglaDePrecio {

    Optional<Ajuste> evaluar(ContextoDePrecio contexto);

    /** Como se describe esta regla para poder guardarla y volver a crearla despues. */
    DescripcionDeRegla describir();
}

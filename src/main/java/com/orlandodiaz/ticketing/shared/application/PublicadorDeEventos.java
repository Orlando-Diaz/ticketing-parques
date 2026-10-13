package com.orlandodiaz.ticketing.shared.application;

import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import java.util.List;

/**
 * PUERTO: quien publica un evento solo dice "esto ocurrio". No sabe quien escucha ni cuantos son.
 * Hoy lo implementa un bus en memoria; manana podria ser RabbitMQ o Kafka sin cambiar los casos de uso.
 */
@FunctionalInterface
public interface PublicadorDeEventos {

    void publicar(EventoDeDominio evento);

    default void publicarTodos(List<? extends EventoDeDominio> eventos) {
        eventos.forEach(this::publicar);
    }
}

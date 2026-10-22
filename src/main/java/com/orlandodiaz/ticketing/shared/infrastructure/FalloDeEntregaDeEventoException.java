package com.orlandodiaz.ticketing.shared.infrastructure;

import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;

/** Al menos un suscriptor fallo al recibir el evento. La causa es el primer fallo; los demas van como suprimidos. */
public class FalloDeEntregaDeEventoException extends RuntimeException {

    public FalloDeEntregaDeEventoException(EventoDeDominio evento, RuntimeException causa) {
        super("Fallo al entregar " + evento.getClass().getSimpleName() + ": " + causa.getMessage(), causa);
    }
}

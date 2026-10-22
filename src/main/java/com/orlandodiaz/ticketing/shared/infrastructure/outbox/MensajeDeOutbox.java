package com.orlandodiaz.ticketing.shared.infrastructure.outbox;

/** Una fila del outbox: un evento guardado, pendiente de entregar. */
public record MensajeDeOutbox(long id, String tipo, String payload, int intentos) {
}

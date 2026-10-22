package com.orlandodiaz.ticketing.shared.infrastructure.outbox;

import com.orlandodiaz.ticketing.shared.application.PublicadorDeEventos;
import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Adaptador de PublicadorDeEventos que NO entrega el evento: lo ANOTA en la tabla outbox.
 * Debe llamarse dentro de la transaccion que guarda el agregado; asi "se guardo la venta" y "quedo anotado el
 * evento" son un solo hecho. De la entrega se encarga el {@link DespachadorDeOutbox}.
 */
public class PublicadorConOutbox implements PublicadorDeEventos {

    private final AlmacenDeOutbox outbox;
    private final SerializadorDeEventos serializador;

    public PublicadorConOutbox(AlmacenDeOutbox outbox, SerializadorDeEventos serializador) {
        this.outbox = outbox;
        this.serializador = serializador;
    }

    @Override
    public void publicar(EventoDeDominio evento) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Los eventos se anotan en el outbox dentro de la transaccion que guarda el "
                    + "agregado; sin transaccion se perderia la garantia (usa EjecutorTransaccional)");
        }
        outbox.agregar(serializador.nombreDe(evento), serializador.aJson(evento), evento.ocurridoEn());
    }
}

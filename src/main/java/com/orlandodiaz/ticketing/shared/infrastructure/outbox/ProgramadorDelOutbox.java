package com.orlandodiaz.ticketing.shared.infrastructure.outbox;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * Red de seguridad: cada pocos segundos despacha lo que haya quedado pendiente (por ejemplo porque el proceso se
 * cayo despues de guardar, o porque un suscriptor fallo). En el camino normal el evento sale al instante, justo
 * despues de confirmar la transaccion; esto solo recoge lo que se quedo atras.
 */
public class ProgramadorDelOutbox {

    private final DespachadorDeOutbox despachador;

    public ProgramadorDelOutbox(DespachadorDeOutbox despachador) {
        this.despachador = despachador;
    }

    @Scheduled(fixedDelayString = "${ticketing.outbox.intervalo-ms:5000}")
    public void despachar() {
        despachador.despacharPendientes();
    }
}

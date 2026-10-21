package com.orlandodiaz.ticketing.accesos.application;

import com.orlandodiaz.ticketing.accesos.domain.EstadoPase;
import com.orlandodiaz.ticketing.shared.application.ReintentoPorConflicto;

/** CASO DE USO: anular el pase de una orden cancelada. Idempotente: sin pase o ya anulado, no hace nada. */
public class AnularPaseDeOrden {

    private final RepositorioDePases pases;

    public AnularPaseDeOrden(RepositorioDePases pases) {
        this.pases = pases;
    }

    public void ejecutar(String ordenRef) {
        ReintentoPorConflicto.ejecutar(ReintentoPorConflicto.INTENTOS_POR_DEFECTO,
                () -> pases.buscarPorOrden(ordenRef)
                        .filter(p -> p.estado() == EstadoPase.ACTIVO)
                        .ifPresent(p -> {
                            p.anular();
                            pases.guardar(p);
                        }));
    }
}

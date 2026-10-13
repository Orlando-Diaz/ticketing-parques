package com.orlandodiaz.ticketing.facturacion.application;

import com.orlandodiaz.ticketing.facturacion.domain.EstadoFactura;

/**
 * CASO DE USO: anular la factura de una orden cancelada. Tambien idempotente:
 * si no hay factura o ya esta anulada, no hace nada.
 */
public class AnularFacturaDeOrden {

    private final RepositorioDeFacturas facturas;

    public AnularFacturaDeOrden(RepositorioDeFacturas facturas) {
        this.facturas = facturas;
    }

    public void ejecutar(String ordenRef) {
        facturas.buscarPorOrden(ordenRef)
                .filter(f -> f.estado() == EstadoFactura.EMITIDA)
                .ifPresent(f -> {
                    f.anular();
                    facturas.guardar(f);
                });
    }
}

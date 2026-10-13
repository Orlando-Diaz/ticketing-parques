package com.orlandodiaz.ticketing.facturacion.application;

import com.orlandodiaz.ticketing.facturacion.domain.Factura;

/** CASO DE USO (consulta): la factura de una orden. */
public class ConsultarFacturaDeOrden {

    private final RepositorioDeFacturas facturas;

    public ConsultarFacturaDeOrden(RepositorioDeFacturas facturas) {
        this.facturas = facturas;
    }

    public Factura ejecutar(String ordenRef) {
        return facturas.buscarPorOrden(ordenRef).orElseThrow(() -> new FacturaNoEncontradaException(ordenRef));
    }
}

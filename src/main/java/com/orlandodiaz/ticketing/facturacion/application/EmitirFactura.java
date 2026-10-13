package com.orlandodiaz.ticketing.facturacion.application;

import com.orlandodiaz.ticketing.facturacion.domain.Factura;

/**
 * CASO DE USO: emitir la factura de una orden.
 * Es IDEMPOTENTE: si la orden ya tiene factura, devuelve esa y no crea otra. Esto importa porque
 * los eventos pueden entregarse dos veces (reintentos), y no queremos facturar doble.
 */
public class EmitirFactura {

    private final RepositorioDeFacturas facturas;
    private final NumeradorDeFacturas numerador;

    public EmitirFactura(RepositorioDeFacturas facturas, NumeradorDeFacturas numerador) {
        this.facturas = facturas;
        this.numerador = numerador;
    }

    public Factura ejecutar(EmitirFacturaComando comando) {
        return facturas.buscarPorOrden(comando.ordenRef()).orElseGet(() -> {
            Factura factura = Factura.emitir(numerador.siguiente(), comando.ordenRef(),
                    comando.parqueId(), comando.lineas(), comando.emitidaEn());
            facturas.guardar(factura);
            return factura;
        });
    }
}

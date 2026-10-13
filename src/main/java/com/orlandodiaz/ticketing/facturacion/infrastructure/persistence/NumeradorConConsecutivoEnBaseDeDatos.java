package com.orlandodiaz.ticketing.facturacion.infrastructure.persistence;

import com.orlandodiaz.ticketing.facturacion.application.NumeradorDeFacturas;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADAPTADOR de salida: numeros de factura consecutivos guardados en la base de datos.
 * El UPDATE bloquea la fila del contador, asi dos facturas simultaneas nunca reciben el mismo numero.
 * Si la transaccion falla, el contador se revierte: no quedan saltos en la numeracion.
 */
public class NumeradorConConsecutivoEnBaseDeDatos implements NumeradorDeFacturas {

    private static final String CONTADOR = "FACTURA";

    private final ConsecutivoJpaRepository jpa;

    public NumeradorConConsecutivoEnBaseDeDatos(ConsecutivoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public String siguiente() {
        jpa.incrementar(CONTADOR);
        return String.format("FAC-%06d", jpa.ultimo(CONTADOR));
    }
}

package com.orlandodiaz.ticketing.facturacion.infrastructure;

import com.orlandodiaz.ticketing.facturacion.application.NumeradorDeFacturas;
import java.util.concurrent.atomic.AtomicInteger;

/** Adaptador: numeros FAC-000001, FAC-000002... (en produccion vendria de una secuencia de la base de datos). */
public class NumeradorEnMemoria implements NumeradorDeFacturas {

    private final AtomicInteger contador = new AtomicInteger();

    @Override
    public String siguiente() {
        return String.format("FAC-%06d", contador.incrementAndGet());
    }
}

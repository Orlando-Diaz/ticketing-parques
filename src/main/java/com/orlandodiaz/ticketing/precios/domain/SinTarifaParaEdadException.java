package com.orlandodiaz.ticketing.precios.domain;

/** El parque no definio ninguna categoria que cubra la edad del visitante. */
public class SinTarifaParaEdadException extends RuntimeException {

    public SinTarifaParaEdadException(int edad) {
        super("El parque no tiene una tarifa definida para visitantes de " + edad + " anios");
    }
}

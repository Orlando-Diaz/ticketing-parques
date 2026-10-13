package com.orlandodiaz.ticketing.catalogo.domain;

public class CupoAgotadoException extends RuntimeException {

    public CupoAgotadoException(int solicitadas, int disponibles) {
        super("No hay cupo suficiente: se pidieron " + solicitadas + " y quedan " + disponibles);
    }
}

package com.orlandodiaz.ticketing.accesos.domain;

public class EntradasDelPaseAgotadasException extends RuntimeException {

    public EntradasDelPaseAgotadasException(int pedidas, int restantes) {
        super(restantes == 0
                ? "Este pase ya uso todas sus entradas"
                : "Se piden " + pedidas + " personas pero a este pase solo le quedan " + restantes + " entradas");
    }
}

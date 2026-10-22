package com.orlandodiaz.ticketing.seguridad.application;

/** Usuario o clave incorrectos. El mensaje es el mismo en ambos casos para no revelar que usuarios existen. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Usuario o clave incorrectos");
    }
}

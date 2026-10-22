package com.orlandodiaz.ticketing.seguridad.application;

public class UsuarioYaExisteException extends RuntimeException {

    public UsuarioYaExisteException(String nombre) {
        super("Ya existe un usuario con el nombre " + nombre);
    }
}

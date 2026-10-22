package com.orlandodiaz.ticketing.seguridad.domain;

/** Una persona del personal que puede iniciar sesion. Guarda la clave YA cifrada, nunca la clave en claro. */
public record Usuario(String nombre, String claveCifrada, Rol rol) {

    public Usuario {
        if (nombre == null || !nombre.matches("[a-z0-9._-]{3,50}")) {
            throw new IllegalArgumentException(
                    "El nombre de usuario debe tener de 3 a 50 caracteres: minusculas, numeros, punto, guion o guion bajo");
        }
        if (claveCifrada == null || claveCifrada.isBlank()) {
            throw new IllegalArgumentException("La clave cifrada es obligatoria");
        }
        if (rol == null) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }
    }
}

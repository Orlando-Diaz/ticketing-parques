package com.orlandodiaz.ticketing.seguridad.domain;

import java.util.regex.Pattern;

/**
 * Una persona que puede iniciar sesion. Guarda la clave YA cifrada, nunca la clave en claro.
 * Para el personal el nombre es un usuario corto ("puerta1"); para un cliente es su correo (en minusculas).
 * {@code nombreVisible} es como se le saluda ("Ana Perez"); es opcional.
 */
public record Usuario(String nombre, String claveCifrada, Rol rol, String nombreVisible) {

    private static final Pattern DE_PERSONAL = Pattern.compile("[a-z0-9._-]{3,50}");
    private static final Pattern CORREO = Pattern.compile("[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}");

    public Usuario {
        if (nombre == null || !(DE_PERSONAL.matcher(nombre).matches() || esCorreo(nombre))) {
            throw new IllegalArgumentException(
                    "El nombre de usuario debe tener de 3 a 50 caracteres: minusculas, numeros, punto, guion o guion bajo");
        }
        if (claveCifrada == null || claveCifrada.isBlank()) {
            throw new IllegalArgumentException("La clave cifrada es obligatoria");
        }
        if (rol == null) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }
        if (rol == Rol.CLIENTE && !esCorreo(nombre)) {
            throw new IllegalArgumentException("Escribe un correo valido");
        }
        if (nombreVisible != null) {
            nombreVisible = nombreVisible.trim();
            if (nombreVisible.isEmpty()) {
                nombreVisible = null;
            } else if (nombreVisible.length() > 100) {
                throw new IllegalArgumentException("El nombre es demasiado largo (maximo 100 caracteres)");
            }
        }
    }

    public Usuario(String nombre, String claveCifrada, Rol rol) {
        this(nombre, claveCifrada, rol, null);
    }

    private static boolean esCorreo(String texto) {
        return texto.length() <= 120 && CORREO.matcher(texto).matches();
    }

    /** Como mostrarlo en pantalla: su nombre si lo dio, si no el nombre de usuario. */
    public String paraMostrar() {
        return nombreVisible != null ? nombreVisible : nombre;
    }
}

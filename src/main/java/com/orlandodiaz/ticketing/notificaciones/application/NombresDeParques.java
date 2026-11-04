package com.orlandodiaz.ticketing.notificaciones.application;

/** PUERTO de salida: el nombre legible de un parque (para el texto del correo). */
public interface NombresDeParques {

    /** El nombre del parque, o el mismo id si no se encuentra. */
    String nombreDe(String parqueId);
}

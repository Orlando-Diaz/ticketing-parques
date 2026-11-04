package com.orlandodiaz.ticketing.notificaciones.application;

/** PUERTO de salida: recuerda que avisos ya se enviaron, para no mandar el mismo correo dos veces. */
public interface RegistroDeNotificaciones {

    boolean yaEnviada(String clave);

    void marcarEnviada(String clave);
}

package com.orlandodiaz.ticketing.notificaciones.application;

/** PUERTO de salida: mandar un correo de texto. Puede ser SMTP real o, sin configurar, solo escribirlo en el log. */
public interface EnviadorDeCorreo {

    void enviar(String para, String asunto, String texto);
}

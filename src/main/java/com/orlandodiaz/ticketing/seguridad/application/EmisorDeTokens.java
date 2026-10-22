package com.orlandodiaz.ticketing.seguridad.application;

import com.orlandodiaz.ticketing.seguridad.domain.Usuario;

/** PUERTO: fabrica el token que el cliente enviara en cada peticion. Hoy es un JWT firmado. */
public interface EmisorDeTokens {

    TokenEmitido emitir(Usuario usuario);

    record TokenEmitido(String valor, long expiraEnSegundos) {
    }
}

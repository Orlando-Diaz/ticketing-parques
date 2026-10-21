package com.orlandodiaz.ticketing.accesos.infrastructure;

import com.orlandodiaz.ticketing.accesos.application.GeneradorDeCodigos;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * ADAPTADOR: codigos de 128 bits aleatorios (SecureRandom), en base64 apto para URLs y QR (22 caracteres).
 * Probar todos los codigos posibles es inviable, a diferencia de un consecutivo (1, 2, 3...).
 */
public class GeneradorDeCodigosSeguro implements GeneradorDeCodigos {

    private final SecureRandom azar = new SecureRandom();

    @Override
    public String siguiente() {
        byte[] bytes = new byte[16];
        azar.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

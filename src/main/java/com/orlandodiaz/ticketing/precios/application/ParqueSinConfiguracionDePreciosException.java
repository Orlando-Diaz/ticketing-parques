package com.orlandodiaz.ticketing.precios.application;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;

public class ParqueSinConfiguracionDePreciosException extends RuntimeException {

    public ParqueSinConfiguracionDePreciosException(ParqueId parqueId) {
        super("El parque '" + parqueId.valor() + "' no tiene configuracion de precios");
    }
}

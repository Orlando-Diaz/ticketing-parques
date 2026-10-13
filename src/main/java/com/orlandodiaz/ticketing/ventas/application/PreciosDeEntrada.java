package com.orlandodiaz.ticketing.ventas.application;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import com.orlandodiaz.ticketing.ventas.domain.Visitante;
import java.time.LocalDate;

/**
 * PUERTO: lo que ventas necesita saber de precios, con SUS propios terminos.
 * Ventas no conoce el modulo precios: un adaptador (en infrastructure) hace la traduccion.
 */
public interface PreciosDeEntrada {
    Dinero precioPara(ParqueId parqueId, Visitante visitante, int tamanoGrupo, LocalDate fechaVisita);
}

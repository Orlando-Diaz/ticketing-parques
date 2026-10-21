package com.orlandodiaz.ticketing.accesos.domain;

import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Evento: pasaron personas por la puerta con un pase. Lleva todo lo que otros modulos (reportes) necesitan.
 * {@code numeroDeIngreso} es la posicion de esta pasada en el historial del pase (0, 1, 2...): junto con la
 * orden identifica de forma unica el ingreso, y asi quien lo reciba dos veces puede ignorar el repetido.
 */
public record IngresoRegistrado(
        String ordenRef,
        String paseId,
        ParqueId parqueId,
        LocalDate fechaVisita,
        int numeroDeIngreso,
        int personas,
        String puerta,
        LocalDateTime ocurridoEn) implements EventoDeDominio {
}

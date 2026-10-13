package com.orlandodiaz.ticketing.ventas.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Evento: se confirmo una orden de compra. Lleva todo lo que otros modulos necesitan saber. */
public record OrdenConfirmada(
        OrdenId ordenId,
        ParqueId parqueId,
        LocalDate fechaVisita,
        List<LineaDeOrden> lineas,
        Dinero total,
        LocalDateTime ocurridoEn) implements EventoDeDominio {
}

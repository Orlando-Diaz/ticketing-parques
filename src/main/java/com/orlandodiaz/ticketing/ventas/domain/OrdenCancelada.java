package com.orlandodiaz.ticketing.ventas.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Evento: se cancelo una orden. */
public record OrdenCancelada(
        OrdenId ordenId,
        ParqueId parqueId,
        LocalDate fechaVisita,
        Dinero total,
        LocalDateTime ocurridoEn) implements EventoDeDominio {
}

package com.orlandodiaz.ticketing.shared.domain;

import java.time.LocalDateTime;

/** Algo importante que YA ocurrio en el negocio (ej. "se confirmo una orden"). Los eventos se nombran en pasado. */
public interface EventoDeDominio {
    LocalDateTime ocurridoEn();
}

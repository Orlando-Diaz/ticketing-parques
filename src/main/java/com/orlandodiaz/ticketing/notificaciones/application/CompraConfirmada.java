package com.orlandodiaz.ticketing.notificaciones.application;

import java.time.LocalDate;
import java.util.List;

/** Lo que el correo necesita saber de una compra (sin depender del modulo de ventas). */
public record CompraConfirmada(String ordenId, String cliente, String parqueId, LocalDate fechaVisita,
                               List<Integer> edades, long total) {
}

package com.orlandodiaz.ticketing.accesos.application;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmitirPaseComando(String ordenRef, ParqueId parqueId, LocalDate fechaVisita, int entradas,
                                LocalDateTime emitidoEn) {
}

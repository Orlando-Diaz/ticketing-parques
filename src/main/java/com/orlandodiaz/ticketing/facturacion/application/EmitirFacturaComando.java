package com.orlandodiaz.ticketing.facturacion.application;

import com.orlandodiaz.ticketing.facturacion.domain.LineaDeFactura;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDateTime;
import java.util.List;

public record EmitirFacturaComando(String ordenRef, ParqueId parqueId, List<LineaDeFactura> lineas, LocalDateTime emitidaEn) {
}

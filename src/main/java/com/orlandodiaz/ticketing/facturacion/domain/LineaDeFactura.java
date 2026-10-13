package com.orlandodiaz.ticketing.facturacion.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;

public record LineaDeFactura(String descripcion, Dinero valor) {
}

package com.orlandodiaz.ticketing.facturacion.application;

import com.orlandodiaz.ticketing.facturacion.domain.Factura;
import java.util.Optional;

public interface RepositorioDeFacturas {

    void guardar(Factura factura);

    Optional<Factura> buscarPorOrden(String ordenRef);
}

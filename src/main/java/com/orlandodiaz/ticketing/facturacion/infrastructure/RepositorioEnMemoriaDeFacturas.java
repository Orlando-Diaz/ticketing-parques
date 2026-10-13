package com.orlandodiaz.ticketing.facturacion.infrastructure;

import com.orlandodiaz.ticketing.facturacion.application.RepositorioDeFacturas;
import com.orlandodiaz.ticketing.facturacion.domain.Factura;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Adaptador en memoria (desarrollo y pruebas). */
public class RepositorioEnMemoriaDeFacturas implements RepositorioDeFacturas {

    private final Map<String, Factura> porOrden = new ConcurrentHashMap<>();

    @Override
    public void guardar(Factura factura) {
        porOrden.put(factura.ordenRef(), factura);
    }

    @Override
    public Optional<Factura> buscarPorOrden(String ordenRef) {
        return Optional.ofNullable(porOrden.get(ordenRef));
    }

    /** Solo para pruebas; no forma parte del puerto. */
    public int cantidad() {
        return porOrden.size();
    }
}

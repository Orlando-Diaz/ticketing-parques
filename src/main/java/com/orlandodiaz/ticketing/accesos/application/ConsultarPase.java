package com.orlandodiaz.ticketing.accesos.application;

import com.orlandodiaz.ticketing.accesos.domain.Pase;

/** CASO DE USO: consultar un pase por su codigo (en la puerta) o por su orden (el comprador pide su QR). */
public class ConsultarPase {

    private final RepositorioDePases pases;

    public ConsultarPase(RepositorioDePases pases) {
        this.pases = pases;
    }

    public Pase porCodigo(String codigo) {
        return pases.buscarPorCodigo(codigo).orElseThrow(PaseNoEncontradoException::paraCodigo);
    }

    public Pase porOrden(String ordenRef) {
        return pases.buscarPorOrden(ordenRef).orElseThrow(() -> PaseNoEncontradoException.paraOrden(ordenRef));
    }
}

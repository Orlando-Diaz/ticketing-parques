package com.orlandodiaz.ticketing.accesos.application;

import com.orlandodiaz.ticketing.accesos.domain.Pase;
import com.orlandodiaz.ticketing.shared.application.ReintentoPorConflicto;

/**
 * CASO DE USO: emitir el pase de una orden confirmada.
 * Es IDEMPOTENTE: si la orden ya tiene pase, devuelve ese y no crea otro (los eventos pueden llegar dos veces).
 * Si dos entregas del mismo evento llegan a la vez, solo una logra guardar; la otra reintenta, encuentra el
 * pase ya creado y lo devuelve.
 */
public class EmitirPase {

    private final RepositorioDePases pases;
    private final GeneradorDeCodigos codigos;

    public EmitirPase(RepositorioDePases pases, GeneradorDeCodigos codigos) {
        this.pases = pases;
        this.codigos = codigos;
    }

    public Pase ejecutar(EmitirPaseComando comando) {
        return ReintentoPorConflicto.obtener(ReintentoPorConflicto.INTENTOS_POR_DEFECTO,
                () -> pases.buscarPorOrden(comando.ordenRef()).orElseGet(() -> {
                    Pase pase = Pase.emitir(codigos.siguiente(), comando.ordenRef(), comando.parqueId(),
                            comando.fechaVisita(), comando.entradas(), comando.emitidoEn());
                    pases.guardar(pase);
                    return pase;
                }));
    }
}

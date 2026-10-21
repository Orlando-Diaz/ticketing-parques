package com.orlandodiaz.ticketing.accesos.application;

import com.orlandodiaz.ticketing.accesos.domain.Pase;
import com.orlandodiaz.ticketing.shared.application.PublicadorDeEventos;
import com.orlandodiaz.ticketing.shared.application.ReintentoPorConflicto;

/**
 * CASO DE USO: validar un QR en la puerta y dejar entrar a N personas.
 * Las reglas (dia correcto, pase activo, entradas disponibles) las aplica Pase.registrarIngreso().
 *
 * CONCURRENCIA: dos puertas pueden escanear el mismo QR al mismo tiempo. El repositorio solo deja guardar al
 * primero; el segundo reintenta releyendo el pase (que ya incluye el ingreso del otro) y asi nunca entran mas
 * personas que las compradas.
 *
 * Los eventos (IngresoRegistrado) se publican DESPUES de guardar y solo del intento que gano: los intentos que
 * chocaron se descartan junto con su copia del pase.
 */
public class ValidarIngreso {

    private final RepositorioDePases pases;
    private final Reloj reloj;
    private final PublicadorDeEventos eventos;

    public ValidarIngreso(RepositorioDePases pases, Reloj reloj, PublicadorDeEventos eventos) {
        this.pases = pases;
        this.reloj = reloj;
        this.eventos = eventos;
    }

    public Pase ejecutar(String codigo, int personas, String puerta) {
        Pase registrado = ReintentoPorConflicto.obtener(ReintentoPorConflicto.INTENTOS_POR_DEFECTO, () -> {
            Pase pase = pases.buscarPorCodigo(codigo).orElseThrow(PaseNoEncontradoException::paraCodigo);
            pase.registrarIngreso(personas, puerta, reloj.ahora());
            pases.guardar(pase);
            return pase;
        });
        eventos.publicarTodos(registrado.sacarEventos());
        return registrado;
    }
}

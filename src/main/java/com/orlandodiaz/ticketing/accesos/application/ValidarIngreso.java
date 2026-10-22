package com.orlandodiaz.ticketing.accesos.application;

import com.orlandodiaz.ticketing.accesos.domain.Pase;
import com.orlandodiaz.ticketing.shared.application.EjecutorTransaccional;
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
 * Los eventos (IngresoRegistrado) se publican en la MISMA transaccion que guarda el pase (con el outbox: quedan
 * anotados o no, junto con el ingreso) y solo del intento que gano: los intentos que chocaron se descartan junto
 * con su copia del pase.
 */
public class ValidarIngreso {

    private final RepositorioDePases pases;
    private final Reloj reloj;
    private final PublicadorDeEventos eventos;
    private final EjecutorTransaccional transaccion;

    public ValidarIngreso(RepositorioDePases pases, Reloj reloj, PublicadorDeEventos eventos) {
        this(pases, reloj, eventos, EjecutorTransaccional.SIN_TRANSACCION);
    }

    public ValidarIngreso(RepositorioDePases pases, Reloj reloj, PublicadorDeEventos eventos,
                          EjecutorTransaccional transaccion) {
        this.pases = pases;
        this.reloj = reloj;
        this.eventos = eventos;
        this.transaccion = transaccion;
    }

    public Pase ejecutar(String codigo, int personas, String puerta) {
        return ReintentoPorConflicto.obtener(ReintentoPorConflicto.INTENTOS_POR_DEFECTO, () -> {
            Pase pase = pases.buscarPorCodigo(codigo).orElseThrow(PaseNoEncontradoException::paraCodigo);
            pase.registrarIngreso(personas, puerta, reloj.ahora());
            transaccion.ejecutar(() -> {
                pases.guardar(pase);
                eventos.publicarTodos(pase.sacarEventos());
                return null;
            });
            return pase;
        });
    }
}

package com.orlandodiaz.ticketing.accesos.application;

import com.orlandodiaz.ticketing.accesos.domain.Pase;
import com.orlandodiaz.ticketing.shared.application.ReintentoPorConflicto;

/**
 * CASO DE USO: validar un QR en la puerta y dejar entrar a N personas.
 * Las reglas (dia correcto, pase activo, entradas disponibles) las aplica Pase.registrarIngreso().
 *
 * CONCURRENCIA: dos puertas pueden escanear el mismo QR al mismo tiempo. El repositorio solo deja guardar al
 * primero; el segundo reintenta releyendo el pase (que ya incluye el ingreso del otro) y asi nunca entran mas
 * personas que las compradas.
 */
public class ValidarIngreso {

    private final RepositorioDePases pases;
    private final Reloj reloj;

    public ValidarIngreso(RepositorioDePases pases, Reloj reloj) {
        this.pases = pases;
        this.reloj = reloj;
    }

    public Pase ejecutar(String codigo, int personas, String puerta) {
        return ReintentoPorConflicto.obtener(ReintentoPorConflicto.INTENTOS_POR_DEFECTO, () -> {
            Pase pase = pases.buscarPorCodigo(codigo).orElseThrow(PaseNoEncontradoException::paraCodigo);
            pase.registrarIngreso(personas, puerta, reloj.ahora());
            pases.guardar(pase);
            return pase;
        });
    }
}

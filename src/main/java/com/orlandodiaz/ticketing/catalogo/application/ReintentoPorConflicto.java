package com.orlandodiaz.ticketing.catalogo.application;

/**
 * Repite una operacion "leer -> modificar -> guardar" cuando otro se nos adelanto.
 * Cada intento vuelve a LEER el cupo actual, asi que ve las ventas que hicieron los demas.
 *
 * Por que termina bien: un conflicto solo ocurre cuando OTRA operacion logro guardar. Si hay N entradas de
 * cupo, a lo sumo habra N guardados exitosos, asi que un comprador no puede perder mas de N veces seguidas.
 */
final class ReintentoPorConflicto {

    static final int INTENTOS_POR_DEFECTO = 10;

    private ReintentoPorConflicto() {
    }

    static void ejecutar(int maximoDeIntentos, Runnable operacion) {
        for (int intento = 1; ; intento++) {
            try {
                operacion.run();
                return;
            } catch (ConflictoDeConcurrenciaException e) {
                if (intento >= maximoDeIntentos) {
                    throw e; // demasiada competencia: que el llamador decida (la API responde 409)
                }
            }
        }
    }
}

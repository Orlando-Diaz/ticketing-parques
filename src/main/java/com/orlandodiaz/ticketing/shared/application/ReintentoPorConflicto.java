package com.orlandodiaz.ticketing.shared.application;

import java.util.function.Supplier;

/**
 * Repite una operacion "leer -> modificar -> guardar" cuando otro se nos adelanto.
 * Cada intento vuelve a LEER el dato actual, asi que ve los cambios que hicieron los demas.
 *
 * Por que termina bien: un conflicto solo ocurre cuando OTRA operacion logro guardar, y cada dato admite
 * un numero finito de cambios utiles (N entradas de cupo, una sola cancelacion por orden), asi que un
 * comprador no puede perder indefinidamente.
 */
public final class ReintentoPorConflicto {

    public static final int INTENTOS_POR_DEFECTO = 10;

    private ReintentoPorConflicto() {
    }

    public static void ejecutar(int maximoDeIntentos, Runnable operacion) {
        obtener(maximoDeIntentos, () -> {
            operacion.run();
            return null;
        });
    }

    public static <T> T obtener(int maximoDeIntentos, Supplier<T> operacion) {
        for (int intento = 1; ; intento++) {
            try {
                return operacion.get();
            } catch (ConflictoDeConcurrenciaException e) {
                if (intento >= maximoDeIntentos) {
                    throw e; // demasiada competencia: que el llamador decida (la API responde 409)
                }
            }
        }
    }
}

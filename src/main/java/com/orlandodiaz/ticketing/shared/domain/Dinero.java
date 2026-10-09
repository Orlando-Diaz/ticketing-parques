package com.orlandodiaz.ticketing.shared.domain;

/**
 * Value Object: dinero en pesos colombianos (sin decimales).
 * Es inmutable: cada operacion devuelve un Dinero nuevo.
 * Usamos long (no double) para no tener errores de redondeo con plata.
 */
public record Dinero(long pesos) implements Comparable<Dinero> {

    public static final Dinero CERO = new Dinero(0);

    public Dinero {
        if (pesos < 0) {
            throw new IllegalArgumentException("El dinero no puede ser negativo: " + pesos);
        }
    }

    public static Dinero pesos(long pesos) {
        return new Dinero(pesos);
    }

    public Dinero sumar(Dinero otro) {
        return new Dinero(Math.addExact(pesos, otro.pesos));
    }

    public Dinero restar(Dinero otro) {
        if (otro.pesos > pesos) {
            throw new IllegalArgumentException("No se puede restar " + otro.pesos + " de " + pesos);
        }
        return new Dinero(pesos - otro.pesos);
    }

    /** Devuelve el porcentaje indicado (0 a 100) de este valor, redondeando hacia abajo. */
    public Dinero porcentaje(int porcentaje) {
        if (porcentaje < 0 || porcentaje > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100: " + porcentaje);
        }
        return new Dinero(pesos * porcentaje / 100);
    }

    @Override
    public int compareTo(Dinero otro) {
        return Long.compare(pesos, otro.pesos);
    }
}

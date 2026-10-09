package com.orlandodiaz.ticketing.precios.domain;

/**
 * Categoria de visitante definida por cada parque (ej. "Nino" de 3 a 10 anios).
 * Los limites de edad son inclusivos.
 */
public record CategoriaVisitante(String nombre, int edadMinima, int edadMaxima) {

    public CategoriaVisitante {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("La categoria necesita un nombre");
        }
        if (edadMinima < 0 || edadMaxima < edadMinima) {
            throw new IllegalArgumentException(
                    "Rango de edad invalido para " + nombre + ": " + edadMinima + "-" + edadMaxima);
        }
    }

    /** Categoria sin limite superior, ej. "Adulto mayor" desde los 60. */
    public static CategoriaVisitante desde(String nombre, int edadMinima) {
        return new CategoriaVisitante(nombre, edadMinima, Integer.MAX_VALUE);
    }

    public boolean incluye(int edad) {
        return edad >= edadMinima && edad <= edadMaxima;
    }

    public boolean seSolapaCon(CategoriaVisitante otra) {
        return edadMinima <= otra.edadMaxima && otra.edadMinima <= edadMaxima;
    }
}

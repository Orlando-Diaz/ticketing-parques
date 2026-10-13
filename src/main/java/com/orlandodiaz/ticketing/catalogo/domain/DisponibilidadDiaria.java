package com.orlandodiaz.ticketing.catalogo.domain;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Cupo de un parque en una fecha concreta. Es la entidad que protege la regla central:
 * NUNCA se pueden tener mas entradas vendidas que el cupo maximo.
 *
 * La regla vive aqui adentro (no en un servicio ni en el controlador), asi nadie puede saltarsela:
 * el unico camino para cambiar las vendidas es reservar() o liberar().
 */
public class DisponibilidadDiaria {

    private final ParqueId parqueId;
    private final LocalDate fecha;
    private int cupoMaximo;
    private int vendidas;

    private DisponibilidadDiaria(ParqueId parqueId, LocalDate fecha, int cupoMaximo, int vendidas) {
        this.parqueId = Objects.requireNonNull(parqueId, "El parque es obligatorio");
        this.fecha = Objects.requireNonNull(fecha, "La fecha es obligatoria");
        if (cupoMaximo < 0) {
            throw new IllegalArgumentException("El cupo maximo no puede ser negativo");
        }
        if (vendidas < 0 || vendidas > cupoMaximo) {
            throw new IllegalArgumentException("Las vendidas deben estar entre 0 y el cupo maximo");
        }
        this.cupoMaximo = cupoMaximo;
        this.vendidas = vendidas;
    }

    public static DisponibilidadDiaria nueva(ParqueId parqueId, LocalDate fecha, int cupoMaximo) {
        return new DisponibilidadDiaria(parqueId, fecha, cupoMaximo, 0);
    }

    /** Para que la capa de persistencia reconstruya una disponibilidad ya guardada. */
    public static DisponibilidadDiaria reconstruir(ParqueId parqueId, LocalDate fecha, int cupoMaximo, int vendidas) {
        return new DisponibilidadDiaria(parqueId, fecha, cupoMaximo, vendidas);
    }

    public void reservar(int cantidad) {
        validarCantidad(cantidad);
        if (cantidad > disponibles()) {
            throw new CupoAgotadoException(cantidad, disponibles());
        }
        vendidas += cantidad;
    }

    /** Devuelve cupo, por ejemplo si se cancela una compra. */
    public void liberar(int cantidad) {
        validarCantidad(cantidad);
        if (cantidad > vendidas) {
            throw new IllegalArgumentException("No se pueden liberar " + cantidad + " si solo hay " + vendidas + " vendidas");
        }
        vendidas -= cantidad;
    }

    /** Cambia el cupo del dia, pero nunca por debajo de lo que ya se vendio. */
    public void ajustarCupo(int nuevoCupo) {
        if (nuevoCupo < vendidas) {
            throw new IllegalArgumentException(
                    "El nuevo cupo (" + nuevoCupo + ") no puede ser menor que las entradas ya vendidas (" + vendidas + ")");
        }
        this.cupoMaximo = nuevoCupo;
    }

    public int disponibles() {
        return cupoMaximo - vendidas;
    }

    public ParqueId parqueId() {
        return parqueId;
    }

    public LocalDate fecha() {
        return fecha;
    }

    public int cupoMaximo() {
        return cupoMaximo;
    }

    public int vendidas() {
        return vendidas;
    }

    private static void validarCantidad(int cantidad) {
        if (cantidad < 1) {
            throw new IllegalArgumentException("La cantidad debe ser al menos 1");
        }
    }
}

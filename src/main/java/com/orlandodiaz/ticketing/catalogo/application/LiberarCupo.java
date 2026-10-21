package com.orlandodiaz.ticketing.catalogo.application;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;

/**
 * CASO DE USO: devolver cupo (cuando se cancela una orden o falla una compra).
 * Igual que ReservarCupo, reintenta si otro modifico el mismo cupo al mismo tiempo.
 */
public class LiberarCupo {

    private final RepositorioDeDisponibilidad disponibilidades;
    private final int maximoDeIntentos;

    public LiberarCupo(RepositorioDeDisponibilidad disponibilidades) {
        this(disponibilidades, ReintentoPorConflicto.INTENTOS_POR_DEFECTO);
    }

    public LiberarCupo(RepositorioDeDisponibilidad disponibilidades, int maximoDeIntentos) {
        if (maximoDeIntentos < 1) {
            throw new IllegalArgumentException("Se necesita al menos 1 intento");
        }
        this.disponibilidades = disponibilidades;
        this.maximoDeIntentos = maximoDeIntentos;
    }

    public void ejecutar(ParqueId parqueId, LocalDate fecha, int cantidad) {
        ReintentoPorConflicto.ejecutar(maximoDeIntentos, () -> {
            var disponibilidad = disponibilidades.buscar(parqueId, fecha)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No hay reservas para el parque '" + parqueId.valor() + "' el " + fecha));
            disponibilidad.liberar(cantidad);
            disponibilidades.guardar(disponibilidad);
        });
    }
}

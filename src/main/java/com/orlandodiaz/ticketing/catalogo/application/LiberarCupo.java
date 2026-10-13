package com.orlandodiaz.ticketing.catalogo.application;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;

/** CASO DE USO: devolver cupo (cuando se cancela una orden o falla una compra). */
public class LiberarCupo {

    private final RepositorioDeDisponibilidad disponibilidades;

    public LiberarCupo(RepositorioDeDisponibilidad disponibilidades) {
        this.disponibilidades = disponibilidades;
    }

    public void ejecutar(ParqueId parqueId, LocalDate fecha, int cantidad) {
        var disponibilidad = disponibilidades.buscar(parqueId, fecha)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay reservas para el parque '" + parqueId.valor() + "' el " + fecha));
        disponibilidad.liberar(cantidad);
        disponibilidades.guardar(disponibilidad);
    }
}

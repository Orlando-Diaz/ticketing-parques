package com.orlandodiaz.ticketing.ventas.infrastructure;

import com.orlandodiaz.ticketing.catalogo.application.LiberarCupo;
import com.orlandodiaz.ticketing.catalogo.application.ReservarCupo;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import com.orlandodiaz.ticketing.ventas.application.CupoDeParque;
import java.time.LocalDate;

/** ADAPTADOR que conecta ventas con el modulo catalogo (el unico punto de contacto). */
public class CupoDeParqueDesdeModuloCatalogo implements CupoDeParque {

    private final ReservarCupo reservarCupo;
    private final LiberarCupo liberarCupo;

    public CupoDeParqueDesdeModuloCatalogo(ReservarCupo reservarCupo, LiberarCupo liberarCupo) {
        this.reservarCupo = reservarCupo;
        this.liberarCupo = liberarCupo;
    }

    @Override
    public void reservar(ParqueId parqueId, LocalDate fecha, int cantidad) {
        reservarCupo.ejecutar(parqueId, fecha, cantidad);
    }

    @Override
    public void liberar(ParqueId parqueId, LocalDate fecha, int cantidad) {
        liberarCupo.ejecutar(parqueId, fecha, cantidad);
    }
}

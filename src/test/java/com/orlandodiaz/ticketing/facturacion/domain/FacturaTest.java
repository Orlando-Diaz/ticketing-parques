package com.orlandodiaz.ticketing.facturacion.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class FacturaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 13, 10, 0);
    private static final ParqueId PARQUE = new ParqueId("parque-a");

    private Factura factura() {
        return Factura.emitir("FAC-000001", "orden-1", PARQUE, List.of(
                new LineaDeFactura("Entrada 1", Dinero.pesos(25_000)),
                new LineaDeFactura("Entrada 2", Dinero.pesos(50_000))), AHORA);
    }

    @Test
    void emitirSumaLasLineasYQuedaEmitida() {
        Factura f = factura();
        assertThat(f.total()).isEqualTo(Dinero.pesos(75_000));
        assertThat(f.estado()).isEqualTo(EstadoFactura.EMITIDA);
    }

    @Test
    void sePuedeAnularUnaVez() {
        Factura f = factura();
        f.anular();
        assertThat(f.estado()).isEqualTo(EstadoFactura.ANULADA);
        assertThatThrownBy(f::anular).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void exigeAlMenosUnaLinea() {
        assertThatThrownBy(() -> Factura.emitir("FAC-000001", "orden-1", PARQUE, List.of(), AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exigeNumeroYReferenciaDeOrden() {
        var lineas = List.of(new LineaDeFactura("Entrada", Dinero.pesos(1_000)));
        assertThatThrownBy(() -> Factura.emitir(" ", "orden-1", PARQUE, lineas, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Factura.emitir("FAC-1", null, PARQUE, lineas, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

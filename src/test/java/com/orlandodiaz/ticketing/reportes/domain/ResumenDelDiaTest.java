package com.orlandodiaz.ticketing.reportes.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumenDelDiaTest {

    private static final ParqueId PARQUE = new ParqueId("parque-a");
    private static final ParqueId OTRO_PARQUE = new ParqueId("parque-b");
    private static final LocalDate DIA = LocalDate.of(2026, 10, 20);
    private static final LocalDateTime MOMENTO = DIA.atTime(10, 0);

    private static OrdenReportada orden(String ref, ParqueId parque, LocalDate dia, int entradas, long total, boolean cancelada) {
        return new OrdenReportada(ref, parque, dia, entradas, Dinero.pesos(total), cancelada, MOMENTO.minusDays(5));
    }

    private static IngresoReportado ingreso(String ref, int numero, ParqueId parque, LocalDate dia, int personas, String puerta) {
        return new IngresoReportado(ref, numero, parque, dia, personas, puerta, MOMENTO);
    }

    @Test
    void unDiaSinActividadEsTodoCero() {
        var resumen = ResumenDelDia.de(PARQUE, DIA, List.of(), List.of());

        assertThat(resumen.entradasVendidas()).isZero();
        assertThat(resumen.ventas()).isEqualTo(Dinero.CERO);
        assertThat(resumen.ingresaron()).isZero();
        assertThat(resumen.sinIngresar()).isZero();
        assertThat(resumen.ingresosPorPuerta()).isEmpty();
    }

    @Test
    void suma_lasVentasVigentesYSeparaLasCanceladas() {
        var resumen = ResumenDelDia.de(PARQUE, DIA, List.of(
                orden("o1", PARQUE, DIA, 3, 125_000, false),
                orden("o2", PARQUE, DIA, 2, 100_000, true),
                orden("o3", PARQUE, DIA, 1, 50_000, false)), List.of());

        assertThat(resumen.ordenesVigentes()).isEqualTo(2);
        assertThat(resumen.entradasVendidas()).isEqualTo(4);
        assertThat(resumen.ventas()).isEqualTo(Dinero.pesos(175_000));
        assertThat(resumen.ordenesCanceladas()).isEqualTo(1);
        assertThat(resumen.entradasCanceladas()).isEqualTo(2);
    }

    @Test
    void cuentaLosIngresosPorPuertaYLosQueFaltanPorEntrar() {
        var resumen = ResumenDelDia.de(PARQUE, DIA,
                List.of(orden("o1", PARQUE, DIA, 5, 250_000, false)),
                List.of(ingreso("o1", 0, PARQUE, DIA, 2, "sur"), ingreso("o1", 1, PARQUE, DIA, 1, "norte"),
                        ingreso("o1", 2, PARQUE, DIA, 1, "sur")));

        assertThat(resumen.ingresaron()).isEqualTo(4);
        assertThat(resumen.sinIngresar()).isEqualTo(1);
        assertThat(resumen.ingresosPorPuerta()).containsEntry("sur", 3).containsEntry("norte", 1);
        assertThat(resumen.ingresosPorPuerta().keySet()).containsExactly("norte", "sur"); // ordenado
    }

    @Test
    void ignoraOtrosParquesYOtrosDias() {
        var resumen = ResumenDelDia.de(PARQUE, DIA, List.of(
                orden("o1", PARQUE, DIA, 2, 80_000, false),
                orden("o2", OTRO_PARQUE, DIA, 9, 900_000, false),
                orden("o3", PARQUE, DIA.plusDays(1), 7, 700_000, false)),
                List.of(ingreso("o1", 0, PARQUE, DIA, 1, "norte"),
                        ingreso("o2", 0, OTRO_PARQUE, DIA, 9, "norte"),
                        ingreso("o3", 0, PARQUE, DIA.plusDays(1), 7, "norte")));

        assertThat(resumen.entradasVendidas()).isEqualTo(2);
        assertThat(resumen.ventas()).isEqualTo(Dinero.pesos(80_000));
        assertThat(resumen.ingresaron()).isEqualTo(1);
    }

    @Test
    void sinIngresarNuncaEsNegativoAunqueEntraranDeUnaOrdenCancelada() {
        // 2 personas ya entraron y despues se cancelo la orden: ya entraron, pero la venta no cuenta
        var resumen = ResumenDelDia.de(PARQUE, DIA,
                List.of(orden("o1", PARQUE, DIA, 2, 100_000, true)),
                List.of(ingreso("o1", 0, PARQUE, DIA, 2, "norte")));

        assertThat(resumen.entradasVendidas()).isZero();
        assertThat(resumen.ingresaron()).isEqualTo(2);
        assertThat(resumen.sinIngresar()).isZero();
    }
}

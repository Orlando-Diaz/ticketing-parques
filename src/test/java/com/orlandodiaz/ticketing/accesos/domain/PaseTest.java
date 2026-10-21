package com.orlandodiaz.ticketing.accesos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaseTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 20);
    private static final LocalDateTime EL_DIA_A_LAS_10 = DIA.atTime(10, 0);

    private static Pase paseDe(int entradas) {
        return Pase.emitir("codigo-1", "orden-1", new ParqueId("parque-a"), DIA, entradas, DIA.minusDays(7).atTime(9, 0));
    }

    @Test
    void unPaseNuevoEstaActivoYSinIngresos() {
        Pase pase = paseDe(4);

        assertThat(pase.estado()).isEqualTo(EstadoPase.ACTIVO);
        assertThat(pase.entradasTotales()).isEqualTo(4);
        assertThat(pase.entradasUsadas()).isZero();
        assertThat(pase.entradasRestantes()).isEqualTo(4);
        assertThat(pase.version()).isNull();
    }

    @Test
    void elGrupoPuedeEntrarEnVariasPasadas() {
        Pase pase = paseDe(4);

        pase.registrarIngreso(3, "norte", EL_DIA_A_LAS_10);
        pase.registrarIngreso(1, "sur", EL_DIA_A_LAS_10.plusHours(2));

        assertThat(pase.entradasUsadas()).isEqualTo(4);
        assertThat(pase.entradasRestantes()).isZero();
        assertThat(pase.ingresos()).extracting(Ingreso::puerta).containsExactly("norte", "sur");
    }

    @Test
    void cadaIngresoRegistraUnEventoYSeEntregaUnaSolaVez() {
        Pase pase = paseDe(4);
        pase.registrarIngreso(3, "norte", EL_DIA_A_LAS_10);
        pase.registrarIngreso(1, null, EL_DIA_A_LAS_10.plusHours(1));

        var eventos = pase.sacarEventos();

        assertThat(eventos).hasSize(2);
        var segundo = (IngresoRegistrado) eventos.get(1);
        assertThat(segundo.numeroDeIngreso()).isEqualTo(1);
        assertThat(segundo.puerta()).isEqualTo("principal");
        assertThat(segundo.ordenRef()).isEqualTo("orden-1");
        assertThat(segundo.fechaVisita()).isEqualTo(DIA);
        assertThat(pase.sacarEventos()).isEmpty(); // ya se entregaron
    }

    @Test
    void unIngresoRechazadoNoRegistraEventos() {
        Pase pase = paseDe(1);
        pase.registrarIngreso(1, null, EL_DIA_A_LAS_10);
        pase.sacarEventos();

        assertThatThrownBy(() -> pase.registrarIngreso(1, null, EL_DIA_A_LAS_10))
                .isInstanceOf(EntradasDelPaseAgotadasException.class);

        assertThat(pase.sacarEventos()).isEmpty();
    }

    @Test
    void noDejaEntrarMasPersonasQueLasCompradas() {
        Pase pase = paseDe(3);
        pase.registrarIngreso(2, null, EL_DIA_A_LAS_10);

        assertThatThrownBy(() -> pase.registrarIngreso(2, null, EL_DIA_A_LAS_10))
                .isInstanceOf(EntradasDelPaseAgotadasException.class)
                .hasMessageContaining("solo le quedan 1");
        assertThat(pase.entradasUsadas()).isEqualTo(2); // el intento fallido no cambio nada
    }

    @Test
    void conTodasLasEntradasUsadasElMensajeLoDiceClaro() {
        Pase pase = paseDe(1);
        pase.registrarIngreso(1, null, EL_DIA_A_LAS_10);

        assertThatThrownBy(() -> pase.registrarIngreso(1, null, EL_DIA_A_LAS_10))
                .isInstanceOf(EntradasDelPaseAgotadasException.class)
                .hasMessageContaining("ya uso todas sus entradas");
    }

    @Test
    void soloValeElDiaDeLaVisita() {
        Pase pase = paseDe(2);

        assertThatThrownBy(() -> pase.registrarIngreso(1, null, DIA.minusDays(1).atTime(10, 0)))
                .isInstanceOf(FechaDePaseIncorrectaException.class)
                .hasMessageContaining("es valido el " + DIA);
        assertThatThrownBy(() -> pase.registrarIngreso(1, null, DIA.plusDays(1).atTime(10, 0)))
                .isInstanceOf(FechaDePaseIncorrectaException.class)
                .hasMessageContaining("vencio");
        assertThat(pase.entradasUsadas()).isZero();
    }

    @Test
    void unPaseAnuladoNoDejaEntrar() {
        Pase pase = paseDe(2);
        pase.anular();

        assertThatThrownBy(() -> pase.registrarIngreso(1, null, EL_DIA_A_LAS_10))
                .isInstanceOf(PaseAnuladoException.class);
    }

    @Test
    void sinNombreDePuertaSeUsaLaPrincipal() {
        Pase pase = paseDe(2);
        pase.registrarIngreso(1, "  ", EL_DIA_A_LAS_10);

        assertThat(pase.ingresos().get(0).puerta()).isEqualTo(Ingreso.PUERTA_POR_DEFECTO);
    }

    @Test
    void entranAlMenosUnaPersona() {
        Pase pase = paseDe(2);

        assertThatThrownBy(() -> pase.registrarIngreso(0, null, EL_DIA_A_LAS_10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noSePuedeReconstruirUnPaseConMasIngresosQueEntradas() {
        var demasiados = List.of(new Ingreso(5, "norte", EL_DIA_A_LAS_10));

        assertThatThrownBy(() -> Pase.reconstruir("id", "codigo", "orden", new ParqueId("parque-a"), DIA, 3,
                EL_DIA_A_LAS_10, EstadoPase.ACTIVO, demasiados, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

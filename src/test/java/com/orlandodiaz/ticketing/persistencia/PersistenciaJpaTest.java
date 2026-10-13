package com.orlandodiaz.ticketing.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeDisponibilidad;
import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeParques;
import com.orlandodiaz.ticketing.catalogo.domain.DisponibilidadDiaria;
import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.facturacion.application.NumeradorDeFacturas;
import com.orlandodiaz.ticketing.facturacion.application.RepositorioDeFacturas;
import com.orlandodiaz.ticketing.facturacion.domain.EstadoFactura;
import com.orlandodiaz.ticketing.facturacion.domain.Factura;
import com.orlandodiaz.ticketing.facturacion.domain.LineaDeFactura;
import com.orlandodiaz.ticketing.precios.application.RepositorioDeConfiguracionDePrecios;
import com.orlandodiaz.ticketing.precios.domain.CategoriaVisitante;
import com.orlandodiaz.ticketing.precios.domain.ConfiguracionDePrecios;
import com.orlandodiaz.ticketing.precios.domain.DatosDeVisita;
import com.orlandodiaz.ticketing.precios.domain.TarifaDeCategoria;
import com.orlandodiaz.ticketing.precios.domain.TarifaPorCategoria;
import com.orlandodiaz.ticketing.precios.domain.TarifaUnica;
import com.orlandodiaz.ticketing.precios.domain.reglas.DescuentoGrupo;
import com.orlandodiaz.ticketing.precios.domain.reglas.DescuentoResidente;
import com.orlandodiaz.ticketing.precios.domain.reglas.RecargoFinDeSemana;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import com.orlandodiaz.ticketing.ventas.application.RepositorioDeOrdenes;
import com.orlandodiaz.ticketing.ventas.domain.EstadoOrden;
import com.orlandodiaz.ticketing.ventas.domain.LineaDeOrden;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba cada adaptador JPA contra una base real (H2 con las migraciones de Flyway):
 * lo que se guarda tiene que volver igual. Cada test usa ids unicos porque la base se comparte.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class PersistenciaJpaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 13, 10, 0);
    private static final LocalDate VISITA = LocalDate.of(2026, 10, 20);

    @Autowired RepositorioDeParques parques;
    @Autowired RepositorioDeDisponibilidad disponibilidades;
    @Autowired RepositorioDeConfiguracionDePrecios configuraciones;
    @Autowired RepositorioDeOrdenes ordenes;
    @Autowired RepositorioDeFacturas facturas;
    @Autowired NumeradorDeFacturas numerador;

    private static ParqueId idUnico(String prefijo) {
        return new ParqueId(prefijo + "-" + UUID.randomUUID());
    }

    @Test
    void parqueSeGuardaYSeRecupera() {
        ParqueId id = idUnico("parque");
        parques.guardar(new Parque(id, "Mi parque", 250));

        Parque leido = parques.buscarPorId(id).orElseThrow();
        assertThat(leido.nombre()).isEqualTo("Mi parque");
        assertThat(leido.cupoDiario()).isEqualTo(250);
        assertThat(parques.buscarPorId(idUnico("no-existe"))).isEmpty();
    }

    @Test
    void disponibilidadConservaLasVendidasYSeActualiza() {
        ParqueId id = idUnico("disp");
        parques.guardar(new Parque(id, "Parque", 100));

        DisponibilidadDiaria d = DisponibilidadDiaria.nueva(id, VISITA, 100);
        d.reservar(30);
        disponibilidades.guardar(d);

        DisponibilidadDiaria leida = disponibilidades.buscar(id, VISITA).orElseThrow();
        assertThat(leida.vendidas()).isEqualTo(30);
        assertThat(leida.disponibles()).isEqualTo(70);

        leida.reservar(20);
        disponibilidades.guardar(leida);
        assertThat(disponibilidades.buscar(id, VISITA).orElseThrow().vendidas()).isEqualTo(50);
        assertThat(disponibilidades.buscar(id, VISITA.plusDays(1))).isEmpty();
    }

    @Test
    void configuracionDePreciosSeRecuperaConElMismoComportamientoYSePuedeReemplazar() {
        ParqueId id = idUnico("precios");
        var original = new ConfiguracionDePrecios(id,
                new TarifaPorCategoria(List.of(
                        new TarifaDeCategoria(new CategoriaVisitante("Nino", 3, 10), Dinero.pesos(25_000)),
                        new TarifaDeCategoria(CategoriaVisitante.desde("Adulto", 11), Dinero.pesos(50_000)))),
                List.of(new DescuentoResidente(20), new DescuentoGrupo(10, 15), new RecargoFinDeSemana(10)));
        configuraciones.guardar(original);

        var visitaResidenteEnGrupo = new DatosDeVisita(30, true, 12, LocalDate.of(2026, 10, 14)); // miercoles
        var visitaSabado = new DatosDeVisita(8, false, 1, LocalDate.of(2026, 10, 17));

        var leida = configuraciones.buscarPorParque(id).orElseThrow();
        assertThat(leida.calcular(visitaResidenteEnGrupo).total()).isEqualTo(original.calcular(visitaResidenteEnGrupo).total());
        assertThat(leida.calcular(visitaSabado).total()).isEqualTo(original.calcular(visitaSabado).total());
        assertThat(leida.calcular(visitaSabado).total()).isEqualTo(Dinero.pesos(27_500)); // 25.000 + 10 %

        // Reemplazo completo: ahora tarifa unica y sin reglas
        configuraciones.guardar(new ConfiguracionDePrecios(id, new TarifaUnica(Dinero.pesos(40_000)), List.of()));
        var reemplazada = configuraciones.buscarPorParque(id).orElseThrow();
        assertThat(reemplazada.calcular(visitaSabado).total()).isEqualTo(Dinero.pesos(40_000));
        assertThat(reemplazada.reglas()).isEmpty();
    }

    @Test
    void ordenSeGuardaConSusLineasYSuCancelacionPersiste() {
        ParqueId id = idUnico("orden");
        Orden orden = Orden.crear(id, VISITA, List.of(
                new LineaDeOrden(8, false, Dinero.pesos(25_000)),
                new LineaDeOrden(30, true, Dinero.pesos(40_000))), AHORA);
        ordenes.guardar(orden);

        Orden leida = ordenes.buscarPorId(orden.id()).orElseThrow();
        assertThat(leida.total()).isEqualTo(Dinero.pesos(65_000));
        assertThat(leida.lineas()).hasSize(2);
        assertThat(leida.lineas().get(1).residente()).isTrue();
        assertThat(leida.estado()).isEqualTo(EstadoOrden.CONFIRMADA);
        assertThat(leida.sacarEventos()).isEmpty(); // leer de la base no genera eventos

        leida.cancelar(AHORA.plusHours(1));
        ordenes.guardar(leida);

        Orden cancelada = ordenes.buscarPorId(orden.id()).orElseThrow();
        assertThat(cancelada.estado()).isEqualTo(EstadoOrden.CANCELADA);
        assertThat(cancelada.lineas()).hasSize(2); // las lineas siguen ahi
    }

    @Test
    void facturaSeGuardaSeBuscaPorOrdenYSuAnulacionPersiste() {
        String ordenRef = "orden-" + UUID.randomUUID();
        Factura factura = Factura.emitir(numerador.siguiente(), ordenRef, idUnico("fact"), List.of(
                new LineaDeFactura("Entrada 1", Dinero.pesos(25_000)),
                new LineaDeFactura("Entrada 2", Dinero.pesos(50_000))), AHORA);
        facturas.guardar(factura);

        Factura leida = facturas.buscarPorOrden(ordenRef).orElseThrow();
        assertThat(leida.numero()).isEqualTo(factura.numero());
        assertThat(leida.total()).isEqualTo(Dinero.pesos(75_000));
        assertThat(leida.lineas()).hasSize(2);
        assertThat(leida.estado()).isEqualTo(EstadoFactura.EMITIDA);

        leida.anular();
        facturas.guardar(leida);
        assertThat(facturas.buscarPorOrden(ordenRef).orElseThrow().estado()).isEqualTo(EstadoFactura.ANULADA);
        assertThat(facturas.buscarPorOrden("no-existe")).isEmpty();
    }

    @Test
    void elNumeradorEntregaNumerosConsecutivosYDistintos() {
        String primero = numerador.siguiente();
        String segundo = numerador.siguiente();

        assertThat(primero).startsWith("FAC-");
        assertThat(segundo).startsWith("FAC-").isNotEqualTo(primero);
        long a = Long.parseLong(primero.substring(4));
        long b = Long.parseLong(segundo.substring(4));
        assertThat(b).isEqualTo(a + 1);
    }
}

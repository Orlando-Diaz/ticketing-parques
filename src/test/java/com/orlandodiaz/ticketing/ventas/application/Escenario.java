package com.orlandodiaz.ticketing.ventas.application;

import com.orlandodiaz.ticketing.catalogo.application.ConsultarCupoDisponible;
import com.orlandodiaz.ticketing.catalogo.application.LiberarCupo;
import com.orlandodiaz.ticketing.catalogo.application.ReservarCupo;
import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.catalogo.infrastructure.RepositorioEnMemoriaDeDisponibilidad;
import com.orlandodiaz.ticketing.catalogo.infrastructure.RepositorioEnMemoriaDeParques;
import com.orlandodiaz.ticketing.precios.application.CalcularPrecioDeEntrada;
import com.orlandodiaz.ticketing.precios.domain.CategoriaVisitante;
import com.orlandodiaz.ticketing.precios.domain.ConfiguracionDePrecios;
import com.orlandodiaz.ticketing.precios.domain.TarifaDeCategoria;
import com.orlandodiaz.ticketing.precios.domain.TarifaPorCategoria;
import com.orlandodiaz.ticketing.precios.domain.TarifaUnica;
import com.orlandodiaz.ticketing.precios.infrastructure.RepositorioEnMemoriaDeConfiguracionDePrecios;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import com.orlandodiaz.ticketing.ventas.infrastructure.CupoDeParqueDesdeModuloCatalogo;
import com.orlandodiaz.ticketing.ventas.infrastructure.PreciosDeEntradaDesdeModuloPrecios;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Arma los tres modulos conectados con sus adaptadores, todo en memoria.
 * Parque A: cupo 100, precios por categoria (nino 25.000 / adulto 50.000).
 * Parque B: cupo 2, tarifa unica de 40.000.
 */
class Escenario {

    static final ParqueId PARQUE_A = new ParqueId("parque-a");
    static final ParqueId PARQUE_B = new ParqueId("parque-b");
    static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 13, 10, 0);
    static final LocalDate VISITA = LocalDate.of(2026, 10, 20);

    final ComprarEntradas comprar;
    final CancelarOrden cancelar;
    final ConsultarCupoDisponible consultarCupo;

    Escenario(RepositorioDeOrdenes ordenes) {
        var parques = new RepositorioEnMemoriaDeParques();
        var disponibilidades = new RepositorioEnMemoriaDeDisponibilidad();
        parques.guardar(new Parque(PARQUE_A, "Parque A", 100));
        parques.guardar(new Parque(PARQUE_B, "Parque B", 2));

        var configuraciones = new RepositorioEnMemoriaDeConfiguracionDePrecios();
        configuraciones.guardar(new ConfiguracionDePrecios(PARQUE_A,
                new TarifaPorCategoria(List.of(
                        new TarifaDeCategoria(new CategoriaVisitante("Nino", 3, 10), Dinero.pesos(25_000)),
                        new TarifaDeCategoria(CategoriaVisitante.desde("Adulto", 11), Dinero.pesos(50_000)))),
                List.of()));
        configuraciones.guardar(new ConfiguracionDePrecios(PARQUE_B,
                new TarifaUnica(Dinero.pesos(40_000)), List.of()));

        var precios = new PreciosDeEntradaDesdeModuloPrecios(new CalcularPrecioDeEntrada(configuraciones));
        var cupo = new CupoDeParqueDesdeModuloCatalogo(
                new ReservarCupo(parques, disponibilidades), new LiberarCupo(disponibilidades));
        Reloj reloj = () -> AHORA;

        this.comprar = new ComprarEntradas(precios, cupo, ordenes, reloj);
        this.cancelar = new CancelarOrden(ordenes, cupo, reloj);
        this.consultarCupo = new ConsultarCupoDisponible(parques, disponibilidades);
    }
}

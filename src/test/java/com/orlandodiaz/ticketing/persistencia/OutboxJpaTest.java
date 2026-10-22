package com.orlandodiaz.ticketing.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.shared.application.PublicadorDeEventos;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import com.orlandodiaz.ticketing.shared.infrastructure.outbox.AlmacenDeOutbox;
import com.orlandodiaz.ticketing.shared.infrastructure.outbox.DespachadorDeOutbox;
import com.orlandodiaz.ticketing.shared.infrastructure.outbox.SerializadorDeEventos;
import com.orlandodiaz.ticketing.ventas.domain.OrdenCancelada;
import com.orlandodiaz.ticketing.ventas.domain.OrdenConfirmada;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** El outbox contra la base real (H2): atomicidad con la transaccion, reintentos y filas fallidas. */
@SpringBootTest
@ActiveProfiles("test")
class OutboxJpaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 13, 10, 0);

    @Autowired
    private PublicadorDeEventos publicador; // el principal: el que anota en el outbox

    @Autowired
    private AlmacenDeOutbox outbox;

    @Autowired
    private SerializadorDeEventos serializador;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager manager;

    private TransactionTemplate transaccion;

    @BeforeEach
    void limpiar() {
        jdbc.update("delete from outbox");
        transaccion = new TransactionTemplate(manager);
    }

    private static OrdenCancelada evento(String id) {
        return new OrdenCancelada(new OrdenId(id), new ParqueId("parque-a"), LocalDate.of(2026, 10, 20),
                Dinero.pesos(40_000), AHORA);
    }

    @Test
    void siLaTransaccionConfirmaElEventoQuedaPendiente() {
        transaccion.executeWithoutResult(t -> publicador.publicar(evento("o-1")));

        assertThat(outbox.contarPendientes()).isEqualTo(1);
    }

    @Test
    void siLaTransaccionSeDeshaceElEventoDesaparece() {
        assertThatThrownBy(() -> transaccion.executeWithoutResult(t -> {
            publicador.publicar(evento("o-2"));
            throw new IllegalStateException("fallo la venta");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(outbox.contarPendientes()).isZero();
    }

    @Test
    void sinTransaccionNoSePermitePublicar() {
        assertThatThrownBy(() -> publicador.publicar(evento("o-3"))).isInstanceOf(IllegalStateException.class);
        assertThat(outbox.contarPendientes()).isZero();
    }

    @Test
    void elDespachadorEntregaLoPendienteYLoMarcaPublicado() {
        var recibidos = new ArrayList<OrdenCancelada>();
        var bus = new BusDeEventosEnMemoria();
        bus.suscribir(OrdenCancelada.class, recibidos::add);
        var despachador = new DespachadorDeOutbox(outbox, serializador, bus, 3);
        transaccion.executeWithoutResult(t -> {
            publicador.publicar(evento("o-4"));
            publicador.publicar(evento("o-5"));
        });

        assertThat(despachador.despacharPendientes()).isEqualTo(2);

        assertThat(recibidos).extracting(e -> e.ordenId().valor()).containsExactly("o-4", "o-5"); // en orden
        assertThat(outbox.contarPendientes()).isZero();
        assertThat(despachador.despacharPendientes()).isZero(); // nada se entrega dos veces
    }

    @Test
    void siUnSuscriptorFallaElEventoSeReintentaYSeEntregaDespues() {
        var intentos = new AtomicInteger();
        var recibidos = new ArrayList<OrdenCancelada>();
        var bus = new BusDeEventosEnMemoria();
        bus.suscribir(OrdenCancelada.class, e -> {
            if (intentos.incrementAndGet() == 1) {
                throw new IllegalStateException("suscriptor caido");
            }
            recibidos.add(e);
        });
        var despachador = new DespachadorDeOutbox(outbox, serializador, bus, 3);
        transaccion.executeWithoutResult(t -> publicador.publicar(evento("o-6")));

        assertThat(despachador.despacharPendientes()).isZero();
        assertThat(outbox.contarPendientes()).as("sigue pendiente").isEqualTo(1);

        assertThat(despachador.despacharPendientes()).isEqualTo(1);
        assertThat(recibidos).hasSize(1);
        assertThat(outbox.contarPendientes()).isZero();
    }

    @Test
    void tras3FallosLaFilaQuedaFallidaYNoBloqueaALasDemas() {
        var recibidos = new ArrayList<String>();
        var bus = new BusDeEventosEnMemoria();
        bus.suscribir(OrdenCancelada.class, e -> {
            if (e.ordenId().valor().equals("veneno")) {
                throw new IllegalStateException("siempre falla");
            }
            recibidos.add(e.ordenId().valor());
        });
        var despachador = new DespachadorDeOutbox(outbox, serializador, bus, 3);
        transaccion.executeWithoutResult(t -> {
            publicador.publicar(evento("veneno"));
            publicador.publicar(evento("sano"));
        });

        despachador.despacharPendientes(); // el sano sale aunque el veneno falle
        assertThat(recibidos).containsExactly("sano");
        despachador.despacharPendientes();
        despachador.despacharPendientes();

        assertThat(outbox.contarFallidos()).isEqualTo(1);
        assertThat(outbox.contarPendientes()).isZero();
        assertThat(despachador.despacharPendientes()).isZero(); // ya no se reintenta
    }

    @Test
    void simulaCaidaDelProcesoLoAnotadoSeEntregaAlVolver() {
        // "el proceso murio": el evento quedo anotado pero nadie lo despacho
        transaccion.executeWithoutResult(t -> publicador.publicar(new OrdenConfirmada(new OrdenId("o-7"),
                new ParqueId("parque-a"), LocalDate.of(2026, 10, 20), List.of(), Dinero.CERO, AHORA)));
        assertThat(outbox.contarPendientes()).isEqualTo(1);

        // "el proceso reinicia": un despachador nuevo, con su propio bus, encuentra el evento
        var recibidos = new ArrayList<OrdenConfirmada>();
        var bus = new BusDeEventosEnMemoria();
        bus.suscribir(OrdenConfirmada.class, recibidos::add);
        new DespachadorDeOutbox(outbox, serializador, bus, 3).despacharPendientes();

        assertThat(recibidos).hasSize(1);
        assertThat(recibidos.get(0).ordenId().valor()).isEqualTo("o-7");
    }
}

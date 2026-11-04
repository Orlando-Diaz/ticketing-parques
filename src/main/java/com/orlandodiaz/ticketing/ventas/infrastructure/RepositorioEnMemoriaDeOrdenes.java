package com.orlandodiaz.ticketing.ventas.infrastructure;

import com.orlandodiaz.ticketing.shared.application.ConflictoDeConcurrenciaException;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import com.orlandodiaz.ticketing.ventas.application.RepositorioDeOrdenes;
import com.orlandodiaz.ticketing.ventas.domain.EstadoOrden;
import com.orlandodiaz.ticketing.ventas.domain.LineaDeOrden;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador en memoria (desarrollo y pruebas). Cumple el mismo contrato de concurrencia que el de base de
 * datos: guarda COPIAS (no el objeto que usa quien lo leyo) y rechaza guardar si la version ya cambio.
 */
public class RepositorioEnMemoriaDeOrdenes implements RepositorioDeOrdenes {

    private record Fila(OrdenId id, ParqueId parqueId, LocalDate fechaVisita, List<LineaDeOrden> lineas,
                        LocalDateTime creadaEn, EstadoOrden estado, long version, String cliente) {
    }

    private final Map<OrdenId, Fila> almacen = new ConcurrentHashMap<>();

    @Override
    public void guardar(Orden o) {
        // compute() es atomico por clave: comparar versiones y guardar no se pueden intercalar
        almacen.compute(o.id(), (id, actual) -> {
            if (actual == null) {
                if (o.version() != null) {
                    throw new ConflictoDeConcurrenciaException("La orden ya no existe");
                }
                return new Fila(o.id(), o.parqueId(), o.fechaVisita(), o.lineas(), o.creadaEn(), o.estado(), 0, o.cliente());
            }
            if (o.version() == null || o.version() != actual.version()) {
                throw new ConflictoDeConcurrenciaException("Otra operacion modifico esta orden antes");
            }
            return new Fila(o.id(), o.parqueId(), o.fechaVisita(), o.lineas(), o.creadaEn(), o.estado(),
                    actual.version() + 1, o.cliente());
        });
    }

    @Override
    public Optional<Orden> buscarPorId(OrdenId id) {
        return Optional.ofNullable(almacen.get(id)).map(f -> Orden.reconstruir(
                f.id(), f.parqueId(), f.fechaVisita(), f.lineas(), f.creadaEn(), f.estado(), f.version(), f.cliente()));
    }

    @Override
    public List<Orden> buscarPorCliente(String cliente) {
        return almacen.values().stream()
                .filter(f -> cliente != null && cliente.equals(f.cliente()))
                .sorted(java.util.Comparator.comparing(Fila::creadaEn).reversed())
                .map(f -> Orden.reconstruir(f.id(), f.parqueId(), f.fechaVisita(), f.lineas(), f.creadaEn(),
                        f.estado(), f.version(), f.cliente()))
                .toList();
    }

    /** Solo para pruebas; no forma parte del puerto. */
    public int cantidad() {
        return almacen.size();
    }
}

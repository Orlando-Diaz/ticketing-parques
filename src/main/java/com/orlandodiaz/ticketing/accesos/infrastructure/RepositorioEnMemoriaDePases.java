package com.orlandodiaz.ticketing.accesos.infrastructure;

import com.orlandodiaz.ticketing.accesos.application.RepositorioDePases;
import com.orlandodiaz.ticketing.accesos.domain.EstadoPase;
import com.orlandodiaz.ticketing.accesos.domain.Ingreso;
import com.orlandodiaz.ticketing.accesos.domain.Pase;
import com.orlandodiaz.ticketing.shared.application.ConflictoDeConcurrenciaException;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador en memoria (desarrollo y pruebas). Cumple el mismo contrato que el de base de datos: guarda COPIAS,
 * rechaza guardar si la version ya cambio y rechaza codigos u ordenes repetidos (como los UNIQUE de la tabla).
 * Todo es synchronized: es un doble de prueba sencillo, no pretende ser rapido.
 */
public class RepositorioEnMemoriaDePases implements RepositorioDePases {

    private record Fila(String id, String codigo, String ordenRef, ParqueId parqueId, LocalDate fechaVisita,
                        int entradasTotales, LocalDateTime emitidoEn, EstadoPase estado, List<Ingreso> ingresos,
                        long version) {

        Pase aDominio() {
            return Pase.reconstruir(id, codigo, ordenRef, parqueId, fechaVisita, entradasTotales, emitidoEn,
                    estado, ingresos, version);
        }
    }

    private final Map<String, Fila> almacen = new HashMap<>();

    @Override
    public synchronized Optional<Pase> buscarPorCodigo(String codigo) {
        return almacen.values().stream().filter(f -> f.codigo().equals(codigo)).findFirst().map(Fila::aDominio);
    }

    @Override
    public synchronized Optional<Pase> buscarPorOrden(String ordenRef) {
        return almacen.values().stream().filter(f -> f.ordenRef().equals(ordenRef)).findFirst().map(Fila::aDominio);
    }

    @Override
    public synchronized void guardar(Pase p) {
        Fila actual = almacen.get(p.id());
        long nuevaVersion;
        if (actual == null) {
            if (p.version() != null) {
                throw new ConflictoDeConcurrenciaException("El pase ya no existe");
            }
            boolean repetido = almacen.values().stream()
                    .anyMatch(f -> f.codigo().equals(p.codigo()) || f.ordenRef().equals(p.ordenRef()));
            if (repetido) {
                throw new ConflictoDeConcurrenciaException("Ya existe un pase con ese codigo u orden");
            }
            nuevaVersion = 0;
        } else {
            if (p.version() == null || p.version() != actual.version()) {
                throw new ConflictoDeConcurrenciaException("Otra operacion modifico este pase antes");
            }
            nuevaVersion = actual.version() + 1;
        }
        almacen.put(p.id(), new Fila(p.id(), p.codigo(), p.ordenRef(), p.parqueId(), p.fechaVisita(),
                p.entradasTotales(), p.emitidoEn(), p.estado(), p.ingresos(), nuevaVersion));
    }
}

package com.orlandodiaz.ticketing.shared.infrastructure.outbox;

import com.orlandodiaz.ticketing.shared.application.EjecutorTransaccional;
import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ejecuta el trabajo en una transaccion de Spring y, YA CONFIRMADA, despierta al despachador para que los eventos
 * recien anotados salgan de inmediato. Se dispara despues de cerrar la transaccion (no dentro de un hook
 * "afterCommit") para que los suscriptores corran en transacciones propias y completas.
 */
public class EjecutorTransaccionalSpring implements EjecutorTransaccional {

    private final TransactionTemplate transaccion;
    private final DespachadorDeOutbox despachador;

    public EjecutorTransaccionalSpring(PlatformTransactionManager manager, DespachadorDeOutbox despachador) {
        this.transaccion = new TransactionTemplate(manager);
        this.despachador = despachador;
    }

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        boolean habiaTransaccion = TransactionSynchronizationManager.isActualTransactionActive();
        T resultado = transaccion.execute(estado -> trabajo.get());
        if (!habiaTransaccion) {
            despachador.despacharPendientes();
        }
        return resultado;
    }
}

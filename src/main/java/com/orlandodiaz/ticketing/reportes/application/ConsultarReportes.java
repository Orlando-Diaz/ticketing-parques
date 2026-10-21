package com.orlandodiaz.ticketing.reportes.application;

import com.orlandodiaz.ticketing.reportes.domain.ResumenDelDia;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** CASO DE USO: consultar los reportes de un parque (por fecha de VISITA). */
public class ConsultarReportes {

    /** Tope del rango, para que una consulta no traiga media base de datos. */
    public static final int MAXIMO_DE_DIAS = 366;

    private final AlmacenDeReportes almacen;

    public ConsultarReportes(AlmacenDeReportes almacen) {
        this.almacen = almacen;
    }

    public ResumenDelDia resumenDelDia(ParqueId parqueId, LocalDate fecha) {
        return ResumenDelDia.de(parqueId, fecha,
                almacen.ordenesEntre(parqueId, fecha, fecha), almacen.ingresosEntre(parqueId, fecha, fecha));
    }

    /** Un resumen por cada dia del rango que tenga ordenes o ingresos, de la fecha mas antigua a la mas reciente. */
    public List<ResumenDelDia> resumenPorDia(ParqueId parqueId, LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("'desde' no puede ser posterior a 'hasta'");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAXIMO_DE_DIAS) {
            throw new IllegalArgumentException("El rango maximo es de " + MAXIMO_DE_DIAS + " dias");
        }
        var ordenes = almacen.ordenesEntre(parqueId, desde, hasta).stream()
                .collect(Collectors.groupingBy(o -> o.fechaVisita()));
        var ingresos = almacen.ingresosEntre(parqueId, desde, hasta).stream()
                .collect(Collectors.groupingBy(i -> i.fechaVisita()));

        var dias = new TreeSet<LocalDate>();
        dias.addAll(ordenes.keySet());
        dias.addAll(ingresos.keySet());

        List<ResumenDelDia> resumenes = new ArrayList<>();
        for (LocalDate dia : dias) {
            resumenes.add(ResumenDelDia.de(parqueId, dia, ordenes.getOrDefault(dia, List.of()),
                    ingresos.getOrDefault(dia, List.of())));
        }
        return resumenes;
    }
}

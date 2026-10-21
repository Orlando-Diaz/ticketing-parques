package com.orlandodiaz.ticketing.accesos.infrastructure.web;

import com.orlandodiaz.ticketing.accesos.application.ConsultarPase;
import com.orlandodiaz.ticketing.accesos.application.ValidarIngreso;
import com.orlandodiaz.ticketing.accesos.domain.Pase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccesosController {

    /** personas es Integer (no int) a proposito: asi podemos avisar con un 400 claro si no viene. */
    public record IngresoRequest(String codigo, Integer personas, String puerta) {
    }

    public record IngresoResponse(int personas, String puerta, LocalDateTime momento) {
    }

    public record PaseResponse(String id, String codigo, String ordenId, String parqueId, LocalDate fechaVisita,
                               String estado, int entradasTotales, int entradasUsadas, int entradasRestantes,
                               List<IngresoResponse> ingresos) {

        static PaseResponse desde(Pase p) {
            return new PaseResponse(p.id(), p.codigo(), p.ordenRef(), p.parqueId().valor(), p.fechaVisita(),
                    p.estado().name(), p.entradasTotales(), p.entradasUsadas(), p.entradasRestantes(),
                    p.ingresos().stream()
                            .map(i -> new IngresoResponse(i.personas(), i.puerta(), i.momento())).toList());
        }
    }

    private final ValidarIngreso validarIngreso;
    private final ConsultarPase consultarPase;

    public AccesosController(ValidarIngreso validarIngreso, ConsultarPase consultarPase) {
        this.validarIngreso = validarIngreso;
        this.consultarPase = consultarPase;
    }

    /** La puerta escanea el QR (su codigo) e indica cuantas personas entran. */
    @PostMapping("/api/accesos/ingresos")
    public PaseResponse registrarIngreso(@RequestBody IngresoRequest request) {
        if (request.codigo() == null || request.codigo().isBlank() || request.personas() == null) {
            throw new IllegalArgumentException("Se necesitan codigo y personas");
        }
        return PaseResponse.desde(validarIngreso.ejecutar(request.codigo(), request.personas(), request.puerta()));
    }

    /** Estado de un pase a partir de su codigo (por ejemplo, para que la puerta confirme antes de dejar pasar). */
    @GetMapping("/api/accesos/pases/{codigo}")
    public PaseResponse pasePorCodigo(@PathVariable("codigo") String codigo) {
        return PaseResponse.desde(consultarPase.porCodigo(codigo));
    }

    /** El comprador pide el pase de su orden: el codigo es lo que se dibuja como QR. */
    @GetMapping("/api/ordenes/{ordenId}/pase")
    public PaseResponse paseDeLaOrden(@PathVariable("ordenId") String ordenId) {
        return PaseResponse.desde(consultarPase.porOrden(ordenId));
    }
}

package com.orlandodiaz.ticketing.configuracion;

import com.orlandodiaz.ticketing.accesos.application.PaseNoEncontradoException;
import com.orlandodiaz.ticketing.accesos.domain.EntradasDelPaseAgotadasException;
import com.orlandodiaz.ticketing.accesos.domain.FechaDePaseIncorrectaException;
import com.orlandodiaz.ticketing.accesos.domain.PaseAnuladoException;
import com.orlandodiaz.ticketing.catalogo.application.ParqueNoEncontradoException;
import com.orlandodiaz.ticketing.catalogo.application.ParqueYaExisteException;
import com.orlandodiaz.ticketing.catalogo.domain.CupoAgotadoException;
import com.orlandodiaz.ticketing.facturacion.application.FacturaNoEncontradaException;
import com.orlandodiaz.ticketing.precios.application.ParqueSinConfiguracionDePreciosException;
import com.orlandodiaz.ticketing.precios.domain.SinTarifaParaEdadException;
import com.orlandodiaz.ticketing.shared.application.ConflictoDeConcurrenciaException;
import com.orlandodiaz.ticketing.ventas.application.OrdenNoEncontradaException;
import com.orlandodiaz.ticketing.ventas.domain.FechaDeVisitaInvalidaException;
import com.orlandodiaz.ticketing.ventas.domain.OrdenNoCancelableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del negocio a respuestas HTTP (formato estandar ProblemDetail).
 * El dominio lanza excepciones con significado de negocio; solo AQUI se decide el codigo HTTP.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    @ExceptionHandler({ParqueNoEncontradoException.class, OrdenNoEncontradaException.class,
            ParqueSinConfiguracionDePreciosException.class, FacturaNoEncontradaException.class,
            PaseNoEncontradoException.class})
    ProblemDetail noEncontrado(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({CupoAgotadoException.class, ParqueYaExisteException.class, OrdenNoCancelableException.class,
            PaseAnuladoException.class, EntradasDelPaseAgotadasException.class})
    ProblemDetail conflicto(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Hubo tanta competencia por el mismo cupo que se agotaron los reintentos: el cliente puede volver a intentar. */
    @ExceptionHandler(ConflictoDeConcurrenciaException.class)
    ProblemDetail demasiadaCompetencia(ConflictoDeConcurrenciaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Hay mucha demanda sobre este cupo en este momento. Intenta de nuevo.");
    }

    @ExceptionHandler({FechaDeVisitaInvalidaException.class, SinTarifaParaEdadException.class,
            FechaDePaseIncorrectaException.class})
    ProblemDetail noProcesable(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(422), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail peticionInvalida(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

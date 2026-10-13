package com.orlandodiaz.ticketing.configuracion;

import com.orlandodiaz.ticketing.catalogo.application.ParqueNoEncontradoException;
import com.orlandodiaz.ticketing.catalogo.application.ParqueYaExisteException;
import com.orlandodiaz.ticketing.catalogo.domain.CupoAgotadoException;
import com.orlandodiaz.ticketing.facturacion.application.FacturaNoEncontradaException;
import com.orlandodiaz.ticketing.precios.application.ParqueSinConfiguracionDePreciosException;
import com.orlandodiaz.ticketing.precios.domain.SinTarifaParaEdadException;
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
            ParqueSinConfiguracionDePreciosException.class, FacturaNoEncontradaException.class})
    ProblemDetail noEncontrado(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({CupoAgotadoException.class, ParqueYaExisteException.class, OrdenNoCancelableException.class})
    ProblemDetail conflicto(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({FechaDeVisitaInvalidaException.class, SinTarifaParaEdadException.class})
    ProblemDetail noProcesable(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(422), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail peticionInvalida(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

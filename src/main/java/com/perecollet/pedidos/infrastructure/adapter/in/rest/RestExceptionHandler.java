package com.perecollet.pedidos.infrastructure.adapter.in.rest;

import com.perecollet.pedidos.application.exception.ClienteNoEncontradoException;
import com.perecollet.pedidos.application.exception.PedidoEnConfirmacionException;
import com.perecollet.pedidos.application.exception.PedidoNoEncontradoException;
import com.perecollet.pedidos.application.exception.ProductoNoEncontradoException;
import com.perecollet.pedidos.application.exception.ProveedorLogisticoException;
import com.perecollet.pedidos.application.exception.ProveedorNoDisponibleException;
import com.perecollet.pedidos.application.exception.ProveedorTimeoutException;
import com.perecollet.pedidos.domain.exception.MonedaNoCoincideException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);


    @ExceptionHandler(PedidoNoEncontradoException.class)
    public ProblemDetail noEncontrado(PedidoNoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({ClienteNoEncontradoException.class, ProductoNoEncontradoException.class, MonedaNoCoincideException.class})
    public ProblemDetail referenciaInvalida(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    @ExceptionHandler(PedidoEnConfirmacionException.class)
    public ProblemDetail enConfirmacion(PedidoEnConfirmacionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ProveedorTimeoutException.class)
    public ProblemDetail proveedorTimeout(ProveedorTimeoutException e) {
        log.warn("Timeout del proveedor logístico", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT,
                "El proveedor logístico no respondió a tiempo. El pedido ha quedado en ERROR_ENVIO y se puede volver a confirmar.");
    }

    @ExceptionHandler(ProveedorNoDisponibleException.class)
    public ProblemDetail proveedorNoDisponible(ProveedorNoDisponibleException e) {
        log.warn("Proveedor logístico no disponible", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "El proveedor logístico no está disponible. El pedido ha quedado en ERROR_ENVIO y se puede volver a confirmar.");

    }

    @ExceptionHandler(ProveedorLogisticoException.class)
    public ProblemDetail proveedorError(ProveedorLogisticoException e) {
        log.warn("Respuesta no válida del proveedor logístico", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "El proveedor logístico devolvió una respuesta no válida. El pedido ha quedado en ERROR_ENVIO y se puede volver a confirmar.");

    }
}
package com.perecollet.pedidos.application.exception;

public class ProveedorTimeoutException extends ProveedorNoDisponibleException {
    public ProveedorTimeoutException(String mensaje) {
        super(mensaje);
    }

    public ProveedorTimeoutException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

package com.perecollet.pedidos.application.exception;

public class ProveedorLogisticoException extends RuntimeException {
    public ProveedorLogisticoException(String mensaje) {
        super(mensaje);
    }

    public ProveedorLogisticoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

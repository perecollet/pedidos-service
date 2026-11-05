package com.perecollet.pedidos.application.exception;

public class ProveedorNoDisponibleException extends ProveedorLogisticoException {
    public ProveedorNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public ProveedorNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

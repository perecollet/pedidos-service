package com.perecollet.pedidos.domain.exception;

import com.perecollet.pedidos.domain.model.EstadoPedido;

public class TransicionEstadoInvalidaException extends RuntimeException {
    public TransicionEstadoInvalidaException(EstadoPedido actual, EstadoPedido destino) {
        super("No se puede pasar un pedido de %s a %s".formatted(actual, destino));
    }}

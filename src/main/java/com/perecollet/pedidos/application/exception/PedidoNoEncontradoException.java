package com.perecollet.pedidos.application.exception;

import com.perecollet.pedidos.domain.model.PedidoId;

public class PedidoNoEncontradoException extends RuntimeException {
    public PedidoNoEncontradoException(PedidoId id) {
        super("No existe el pedido %s".formatted(id.valor()));
    }
}

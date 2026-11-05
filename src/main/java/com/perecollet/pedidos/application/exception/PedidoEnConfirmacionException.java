package com.perecollet.pedidos.application.exception;

import com.perecollet.pedidos.domain.model.PedidoId;

public class PedidoEnConfirmacionException extends RuntimeException {
    public PedidoEnConfirmacionException(PedidoId id) {
        super("El pedido %s ya se está confirmando; inténtalo de nuevo en unos segundos".formatted(id.valor()));
    }
}

package com.perecollet.pedidos.domain.model;

import java.util.Objects;
import java.util.UUID;

public record PedidoId(UUID valor) {

    public PedidoId {
        Objects.requireNonNull(valor, "pedidoId");
    }

    public static PedidoId nuevo() {
        return new PedidoId(UUID.randomUUID());
    }
}

package com.perecollet.pedidos.domain.model;

import java.util.Set;

public enum EstadoPedido {
    CREADO,
    CONFIRMANDO,
    CONFIRMADO,
    ERROR_ENVIO;

    private static final Set<EstadoPedido> CONFIRMABLES = Set.of(CREADO, ERROR_ENVIO);

    public static Set<EstadoPedido> confirmables() {
        return CONFIRMABLES;
    }
}

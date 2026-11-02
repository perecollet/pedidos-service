package com.perecollet.pedidos.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ClienteId(UUID valor) {

    public ClienteId {
        Objects.requireNonNull(valor, "clienteId");
    }
}

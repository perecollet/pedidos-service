package com.perecollet.pedidos.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ProductoId(UUID valor) {

    public ProductoId {
        Objects.requireNonNull(valor, "productoId");
    }
}

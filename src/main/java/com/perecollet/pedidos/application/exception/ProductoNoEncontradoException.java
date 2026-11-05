package com.perecollet.pedidos.application.exception;

import com.perecollet.pedidos.domain.model.ProductoId;

public class ProductoNoEncontradoException extends RuntimeException {
    public ProductoNoEncontradoException(ProductoId id) {
        super("No existe el producto %s".formatted(id.valor()));
    }
}

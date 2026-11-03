package com.perecollet.pedidos.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Producto(ProductoId id, String nombre, Dinero precio, BigDecimal pesoKg) {

    public Producto {
        Objects.requireNonNull(id, "productoId");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(precio, "precio");
        Objects.requireNonNull(pesoKg, "pesoKg");

        if (pesoKg.signum() < 0) {
            throw new IllegalArgumentException("El peso del producto no puede ser negativo: " + pesoKg);
        }
    }
}

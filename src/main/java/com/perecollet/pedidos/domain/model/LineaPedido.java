package com.perecollet.pedidos.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record LineaPedido(ProductoId productoId, int cantidad, Dinero precioUnitario, BigDecimal pesoKg) {

    public LineaPedido {
        Objects.requireNonNull(productoId, "productoId");
        Objects.requireNonNull(precioUnitario, "precioUnitario");
        Objects.requireNonNull(pesoKg, "pesoKg");

        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad del producto no puede ser menor o igual que 0: " + cantidad);
        }

        if (pesoKg.signum() < 0) {
            throw new IllegalArgumentException("El peso del producto no puede ser negativo: " + pesoKg);
        }
    }

    public static LineaPedido of(Producto producto, int cantidad) {
        Objects.requireNonNull(producto, "producto");
        return new LineaPedido(producto.id(), cantidad, producto.precio(), producto.pesoKg());
    }

    public Dinero importe() {
        return precioUnitario.multiplicar(cantidad);
    }

    public BigDecimal pesoTotalKg() {
        return pesoKg.multiply(BigDecimal.valueOf(cantidad));
    }
}

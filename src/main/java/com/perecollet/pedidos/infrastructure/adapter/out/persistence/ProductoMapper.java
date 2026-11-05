package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import com.perecollet.pedidos.domain.model.Dinero;
import com.perecollet.pedidos.domain.model.Producto;
import com.perecollet.pedidos.domain.model.ProductoId;

import java.util.Currency;

final class ProductoMapper {

    private ProductoMapper() {
    }

    static Producto toDomain(ProductoEntity entidad) {
        return new Producto(
                new ProductoId(entidad.getId()),
                entidad.getNombre(),
                Dinero.of(entidad.getPrecio(), Currency.getInstance(entidad.getMoneda())),
                entidad.getPesoKg()
        );
    }
}
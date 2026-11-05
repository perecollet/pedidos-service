package com.perecollet.pedidos.application.port.out;

import com.perecollet.pedidos.domain.model.Producto;
import com.perecollet.pedidos.domain.model.ProductoId;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ProductoRepositoryPort {

    Map<ProductoId,Producto> buscarPorIds(Set<ProductoId> productoIds);
}

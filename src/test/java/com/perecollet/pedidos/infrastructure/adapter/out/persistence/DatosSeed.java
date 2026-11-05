package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import com.perecollet.pedidos.domain.model.ClienteId;
import com.perecollet.pedidos.domain.model.ProductoId;

import java.util.UUID;

final class DatosSeed {

    static final ClienteId CLIENTE_ANA = new ClienteId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    static final ProductoId TECLADO = new ProductoId(UUID.fromString("22222222-2222-2222-2222-222222222221"));
    static final ProductoId RATON = new ProductoId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    private DatosSeed() {
    }
}
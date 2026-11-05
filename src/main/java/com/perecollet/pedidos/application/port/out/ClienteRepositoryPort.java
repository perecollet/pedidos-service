package com.perecollet.pedidos.application.port.out;

import com.perecollet.pedidos.domain.model.ClienteId;

public interface ClienteRepositoryPort {

    boolean existe(ClienteId id);
}

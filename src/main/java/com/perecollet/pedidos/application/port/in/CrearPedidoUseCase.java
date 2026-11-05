package com.perecollet.pedidos.application.port.in;

import com.perecollet.pedidos.domain.model.Pedido;

public interface CrearPedidoUseCase {

    Pedido crear(CrearPedidoCommand command);
}

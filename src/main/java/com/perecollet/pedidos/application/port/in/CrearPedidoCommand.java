package com.perecollet.pedidos.application.port.in;

import com.perecollet.pedidos.domain.model.ClienteId;
import com.perecollet.pedidos.domain.model.ProductoId;

import java.util.List;
import java.util.Objects;

public record CrearPedidoCommand(ClienteId clienteId, String codigoPostalDestino, List<LineaCommand> lineas) {
    public CrearPedidoCommand {
        Objects.requireNonNull(clienteId, "clienteId");
        Objects.requireNonNull(codigoPostalDestino, "codigoPostalDestino");
        lineas = List.copyOf(Objects.requireNonNull(lineas, "lineas"));
    }

    public record LineaCommand(ProductoId productoId, int cantidad) {
        public LineaCommand {
            Objects.requireNonNull(productoId, "productoId");
        }
    }
}

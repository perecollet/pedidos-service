package com.perecollet.pedidos.infrastructure.adapter.in.rest;

import com.perecollet.pedidos.application.port.in.CrearPedidoCommand;
import com.perecollet.pedidos.domain.model.ClienteId;
import com.perecollet.pedidos.domain.model.ProductoId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record CrearPedidoRequest(@NotNull UUID clienteId,
                                 @NotBlank String codigoPostalDestino,
                                 @NotEmpty List<@Valid LineaRequest> lineas) {

    record LineaRequest(@NotNull UUID productoId, @Positive int cantidad) { }

    CrearPedidoCommand toCommand() {
        return new CrearPedidoCommand(new ClienteId(clienteId), codigoPostalDestino,
                lineas.stream()
                        .map(l -> new CrearPedidoCommand.LineaCommand(new ProductoId(l.productoId()), l.cantidad()))
                        .toList());
    }
}

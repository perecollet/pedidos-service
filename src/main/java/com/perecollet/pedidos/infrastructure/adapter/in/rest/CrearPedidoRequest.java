package com.perecollet.pedidos.infrastructure.adapter.in.rest;

import com.perecollet.pedidos.application.port.in.CrearPedidoCommand;
import com.perecollet.pedidos.domain.model.ClienteId;
import com.perecollet.pedidos.domain.model.ProductoId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record CrearPedidoRequest(@NotNull UUID clienteId,
                                 @NotNull @Pattern(regexp = "\\d{5}") String codigoPostalDestino,
                                 @NotEmpty List<@NotNull @Valid LineaRequest> lineas) {

    record LineaRequest(@NotNull UUID productoId, @Positive @Max(1000) int cantidad) { }

    CrearPedidoCommand toCommand() {
        return new CrearPedidoCommand(new ClienteId(clienteId), codigoPostalDestino,
                lineas.stream()
                        .map(l -> new CrearPedidoCommand.LineaCommand(new ProductoId(l.productoId()), l.cantidad()))
                        .toList());
    }
}

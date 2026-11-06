package com.perecollet.pedidos.infrastructure.adapter.in.rest;


import com.perecollet.pedidos.application.port.in.ConfirmarPedidoUseCase;
import com.perecollet.pedidos.application.port.in.CrearPedidoUseCase;
import com.perecollet.pedidos.domain.model.Pedido;
import com.perecollet.pedidos.domain.model.PedidoId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final CrearPedidoUseCase crearPedidoUseCase;
    private final ConfirmarPedidoUseCase confirmarPedidoUseCase;

    public PedidoController(CrearPedidoUseCase crearPedidoUseCase, ConfirmarPedidoUseCase confirmarPedidoUseCase) {
        this.crearPedidoUseCase = crearPedidoUseCase;
        this.confirmarPedidoUseCase = confirmarPedidoUseCase;
    }

    @PostMapping
    ResponseEntity<PedidoResponse> crear(@Valid @RequestBody CrearPedidoRequest request) {
        Pedido pedido = crearPedidoUseCase.crear(request.toCommand());
        return ResponseEntity.created(URI.create("/api/pedidos/" + pedido.id().valor()))
                .body(PedidoResponse.from(pedido));
    }

    @PostMapping("/{id}/confirmacion")
    PedidoResponse confirmar(@PathVariable UUID id) {
        return PedidoResponse.from(confirmarPedidoUseCase.confirmar(new PedidoId(id)));
    }
}

package com.perecollet.pedidos.infrastructure.adapter.in.rest;


import com.perecollet.pedidos.application.port.in.ConfirmarPedidoUseCase;
import com.perecollet.pedidos.application.port.in.CrearPedidoUseCase;
import com.perecollet.pedidos.domain.model.PedidoId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    @ResponseStatus(HttpStatus.CREATED)
    PedidoResponse crear(@Valid @RequestBody CrearPedidoRequest request) {
        return PedidoResponse.from(crearPedidoUseCase.crear(request.toCommand()));
    }

    @PostMapping("/{id}/confirmacion")
    PedidoResponse confirmar(@PathVariable UUID id) {
        return PedidoResponse.from(confirmarPedidoUseCase.confirmar(new PedidoId(id)));
    }
}

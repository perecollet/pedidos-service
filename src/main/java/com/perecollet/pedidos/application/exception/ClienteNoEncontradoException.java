package com.perecollet.pedidos.application.exception;

import com.perecollet.pedidos.domain.model.ClienteId;

public class ClienteNoEncontradoException extends RuntimeException {
    public ClienteNoEncontradoException(ClienteId id) {
        super("No existe el cliente %s".formatted(id.valor()));
    }}

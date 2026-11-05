package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import com.perecollet.pedidos.application.port.out.ClienteRepositoryPort;
import com.perecollet.pedidos.domain.model.ClienteId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ClientePersistenceAdapter implements ClienteRepositoryPort {

    private final ClienteJpaRepository clienteJpaRepository;

    public ClientePersistenceAdapter(ClienteJpaRepository clienteJpaRepository) {
        this.clienteJpaRepository = clienteJpaRepository;
    }

    @Override
    public boolean existe(ClienteId id) {
        return clienteJpaRepository.existsById(id.valor());
    }
}

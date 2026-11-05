package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import com.perecollet.pedidos.TestcontainersConfiguration;
import com.perecollet.pedidos.domain.model.ClienteId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static com.perecollet.pedidos.infrastructure.adapter.out.persistence.DatosSeed.CLIENTE_ANA;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ClientePersistenceAdapterTest {

    @Autowired
    private ClientePersistenceAdapter adapter;

    @Test
    void givenClienteDelSeed_whenExiste_thenDevuelveTrue() {
        assertThat(adapter.existe(CLIENTE_ANA)).isTrue();
    }

    @Test
    void givenIdQueNoEstaEnBaseDeDatos_whenExiste_thenDevuelveFalse() {
        ClienteId inexistente = new ClienteId(UUID.randomUUID());

        assertThat(adapter.existe(inexistente)).isFalse();
    }
}
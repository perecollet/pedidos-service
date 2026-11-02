package com.perecollet.pedidos.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentificadoresTest {

    // --- PedidoId: lo genera el dominio ---

    @Test
    void givenNada_whenNuevo_thenGeneraPedidoIdConValor() {
        PedidoId id = PedidoId.nuevo();

        assertThat(id.valor()).isNotNull();
    }

    @Test
    void givenDosLlamadas_whenNuevo_thenGeneraIdsDistintos() {
        assertThat(PedidoId.nuevo()).isNotEqualTo(PedidoId.nuevo());
    }

    @Test
    void givenMismoUuid_whenCrearDosPedidoId_thenSonIguales() {
        UUID uuid = UUID.randomUUID();

        assertThat(new PedidoId(uuid)).isEqualTo(new PedidoId(uuid));
    }

    @Test
    void givenValorNulo_whenCrearPedidoId_thenLanzaNullPointerException() {
        assertThatThrownBy(() -> new PedidoId(null))
                .isInstanceOf(NullPointerException.class);
    }

    // --- ClienteId y ProductoId: vienen de fuera (seed / petición) ---

    @Test
    void givenValorNulo_whenCrearClienteId_thenLanzaNullPointerException() {
        assertThatThrownBy(() -> new ClienteId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void givenValorNulo_whenCrearProductoId_thenLanzaNullPointerException() {
        assertThatThrownBy(() -> new ProductoId(null))
                .isInstanceOf(NullPointerException.class);
    }
}
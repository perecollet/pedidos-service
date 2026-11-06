package com.perecollet.pedidos.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoPedidoTest {

    @Test
    void givenEstados_whenConfirmables_thenSoloCreadoYErrorEnvioAdmitenConfirmacion() {
        assertThat(EstadoPedido.confirmables())
                .containsExactlyInAnyOrder(EstadoPedido.CREADO, EstadoPedido.ERROR_ENVIO);
    }
}

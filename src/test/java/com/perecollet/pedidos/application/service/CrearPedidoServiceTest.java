
package com.perecollet.pedidos.application.service;

import com.perecollet.pedidos.application.exception.ClienteNoEncontradoException;
import com.perecollet.pedidos.application.exception.ProductoNoEncontradoException;
import com.perecollet.pedidos.application.port.in.CrearPedidoCommand;
import com.perecollet.pedidos.application.port.in.CrearPedidoCommand.LineaCommand;
import com.perecollet.pedidos.application.port.out.ClienteRepositoryPort;
import com.perecollet.pedidos.application.port.out.PedidoRepositoryPort;
import com.perecollet.pedidos.application.port.out.ProductoRepositoryPort;
import com.perecollet.pedidos.domain.model.ClienteId;
import com.perecollet.pedidos.domain.model.Dinero;
import com.perecollet.pedidos.domain.model.EstadoPedido;
import com.perecollet.pedidos.domain.model.Pedido;
import com.perecollet.pedidos.domain.model.Producto;
import com.perecollet.pedidos.domain.model.ProductoId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearPedidoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-04T10:00:00Z");
    private static final ClienteId CLIENTE_ID = new ClienteId(UUID.randomUUID());
    private static final String CODIGO_POSTAL = "43840";

    private static final Producto TECLADO = producto("49.99", "1.25");
    private static final Producto RATON = producto("19.90", "0.20");

    @Mock
    private ClienteRepositoryPort clientes;
    @Mock
    private ProductoRepositoryPort productos;
    @Mock
    private PedidoRepositoryPort pedidos;

    private CrearPedidoService servicio;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        servicio = new CrearPedidoService(clientes, productos, pedidos, reloj);
    }

    // --- Helpers ---

    private static Producto producto(String precio, String pesoKg) {
        return new Producto(new ProductoId(UUID.randomUUID()), "Producto de prueba",
                Dinero.euros(new BigDecimal(precio)), new BigDecimal(pesoKg));
    }

    private static Dinero eur(String importe) {
        return Dinero.euros(new BigDecimal(importe));
    }

    /** 49.99 x 2 = 99.98  +  19.90 x 3 = 59.70  →  159.68 EUR */
    private static CrearPedidoCommand commandValido() {
        return new CrearPedidoCommand(CLIENTE_ID, CODIGO_POSTAL, List.of(
                new LineaCommand(TECLADO.id(), 2),
                new LineaCommand(RATON.id(), 3)
        ));
    }

    private void dadoClienteExistente() {
        when(clientes.existe(CLIENTE_ID)).thenReturn(true);
    }

    private void dadoCatalogoConTecladoYRaton() {
        when(productos.buscarPorIds(Set.of(TECLADO.id(), RATON.id())))
                .thenReturn(Map.of(TECLADO.id(), TECLADO, RATON.id(), RATON));
    }

    private Pedido pedidoGuardado() {
        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidos).guardar(captor.capture());
        return captor.getValue();
    }

    // --- Camino feliz ---

    @Test
    void givenClienteYProductosExistentes_whenCrear_thenGuardaPedidoCreadoConFechaDelReloj() {
        dadoClienteExistente();
        dadoCatalogoConTecladoYRaton();

        servicio.crear(commandValido());

        Pedido guardado = pedidoGuardado();
        assertThat(guardado.estado()).isEqualTo(EstadoPedido.CREADO);
        assertThat(guardado.clienteId()).isEqualTo(CLIENTE_ID);
        assertThat(guardado.codigoPostalDestino()).isEqualTo(CODIGO_POSTAL);
        assertThat(guardado.fecha()).isEqualTo(AHORA);
    }

    @Test
    void givenClienteYProductosExistentes_whenCrear_thenLineasUsanPrecioYPesoDelCatalogo() {
        dadoClienteExistente();
        dadoCatalogoConTecladoYRaton();

        servicio.crear(commandValido());

        Pedido guardado = pedidoGuardado();
        assertThat(guardado.lineas()).hasSize(2);
        assertThat(guardado.lineas().get(0).productoId()).isEqualTo(TECLADO.id());
        assertThat(guardado.lineas().get(0).precioUnitario()).isEqualTo(TECLADO.precio());
        assertThat(guardado.lineas().get(0).cantidad()).isEqualTo(2);
        assertThat(guardado.importeProductos()).isEqualTo(eur("159.68"));
    }

    @Test
    void givenClienteYProductosExistentes_whenCrear_thenDevuelveElPedidoGuardado() {
        dadoClienteExistente();
        dadoCatalogoConTecladoYRaton();

        Pedido devuelto = servicio.crear(commandValido());

        assertThat(devuelto).isSameAs(pedidoGuardado());
    }

    @Test
    void givenVariasLineas_whenCrear_thenBuscaTodosLosProductosEnUnaSolaConsulta() {
        dadoClienteExistente();
        dadoCatalogoConTecladoYRaton();

        servicio.crear(commandValido());

        verify(productos).buscarPorIds(Set.of(TECLADO.id(), RATON.id()));
    }

    // --- Errores ---

    @Test
    void givenClienteInexistente_whenCrear_thenLanzaClienteNoEncontradoYNoGuarda() {
        when(clientes.existe(CLIENTE_ID)).thenReturn(false);
        CrearPedidoCommand command = commandValido();

        assertThatThrownBy(() -> servicio.crear(command))
                .isInstanceOf(ClienteNoEncontradoException.class)
                .hasMessageContaining(CLIENTE_ID.valor().toString());

        verifyNoInteractions(productos);
        verify(pedidos, never()).guardar(any());
    }

    @Test
    void givenProductoInexistente_whenCrear_thenLanzaProductoNoEncontradoYNoGuarda() {
        dadoClienteExistente();
        when(productos.buscarPorIds(Set.of(TECLADO.id(), RATON.id())))
                .thenReturn(Map.of(TECLADO.id(), TECLADO));   // falta RATON
        CrearPedidoCommand command = commandValido();

        assertThatThrownBy(() -> servicio.crear(command))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessageContaining(RATON.id().valor().toString());

        verify(pedidos, never()).guardar(any());
    }
}
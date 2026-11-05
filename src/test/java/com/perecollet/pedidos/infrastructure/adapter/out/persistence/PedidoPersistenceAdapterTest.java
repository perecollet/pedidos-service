package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import com.perecollet.pedidos.TestcontainersConfiguration;
import com.perecollet.pedidos.domain.model.Dinero;
import com.perecollet.pedidos.domain.model.EstadoPedido;
import com.perecollet.pedidos.domain.model.LineaPedido;
import com.perecollet.pedidos.domain.model.Pedido;
import com.perecollet.pedidos.domain.model.PedidoId;
import com.perecollet.pedidos.domain.model.Producto;
import com.perecollet.pedidos.domain.model.ProductoId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.perecollet.pedidos.infrastructure.adapter.out.persistence.DatosSeed.CLIENTE_ANA;
import static com.perecollet.pedidos.infrastructure.adapter.out.persistence.DatosSeed.RATON;
import static com.perecollet.pedidos.infrastructure.adapter.out.persistence.DatosSeed.TECLADO;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PedidoPersistenceAdapterTest {

    private static final String CODIGO_POSTAL = "43840";
    private static final Instant FECHA = Instant.parse("2026-11-05T10:00:00Z");

    @Autowired
    private PedidoPersistenceAdapter adapter;

    @Autowired
    private ProductoPersistenceAdapter productos;

    @Autowired
    private JdbcTemplate jdbc;

    // --- Helpers ---

    private static Dinero eur(String importe) {
        return Dinero.euros(new BigDecimal(importe));
    }

    /** Teclado 49.99 x 2 = 99.98  +  Ratón 19.90 x 3 = 59.70  →  159.68 EUR */
    private List<LineaPedido> lineasDelCatalogo() {
        Map<ProductoId, Producto> catalogo = productos.buscarPorIds(Set.of(TECLADO, RATON));
        return List.of(
                LineaPedido.of(catalogo.get(TECLADO), 2),
                LineaPedido.of(catalogo.get(RATON), 3)
        );
    }

    private Pedido pedidoCreado() {
        return Pedido.crear(CLIENTE_ANA, CODIGO_POSTAL, lineasDelCatalogo(), FECHA);
    }

    private Pedido pedidoConfirmado(PedidoId id) {
        return Pedido.reconstituir(id, CLIENTE_ANA, CODIGO_POSTAL, lineasDelCatalogo(), FECHA,
                EstadoPedido.CONFIRMADO, eur("7.95"));
    }

    // --- Ida y vuelta ---

    @Test
    void givenPedidoCreado_whenGuardarYBuscar_thenRecuperaTodosSusDatos() {
        Pedido original = pedidoCreado();

        adapter.guardar(original);
        Pedido leido = adapter.buscarPorId(original.id()).orElseThrow();

        assertThat(leido.id()).isEqualTo(original.id());
        assertThat(leido.clienteId()).isEqualTo(CLIENTE_ANA);
        assertThat(leido.codigoPostalDestino()).isEqualTo(CODIGO_POSTAL);
        assertThat(leido.fecha()).isEqualTo(FECHA);
        assertThat(leido.estado()).isEqualTo(EstadoPedido.CREADO);
        assertThat(leido.costeEnvio()).isEmpty();
        assertThat(leido.importeProductos()).isEqualTo(eur("159.68"));
    }

    @Test
    void givenPedidoConVariasLineas_whenGuardarYBuscar_thenConservaLineasYSuOrden() {
        Pedido original = pedidoCreado();

        adapter.guardar(original);
        Pedido leido = adapter.buscarPorId(original.id()).orElseThrow();

        assertThat(leido.lineas()).hasSize(2);
        assertThat(leido.lineas()).extracting(LineaPedido::productoId).containsExactly(TECLADO, RATON);
        assertThat(leido.lineas()).extracting(LineaPedido::cantidad).containsExactly(2, 3);
        assertThat(leido.lineas().get(0).precioUnitario()).isEqualTo(eur("49.99"));
        assertThat(leido.lineas().get(0).pesoKg()).isEqualByComparingTo("1.25");
    }

    @Test
    void givenPedidoConfirmado_whenGuardarYBuscar_thenRecuperaCosteEnvioYTotal() {
        Pedido original = pedidoConfirmado(PedidoId.nuevo());

        adapter.guardar(original);
        Pedido leido = adapter.buscarPorId(original.id()).orElseThrow();

        assertThat(leido.estado()).isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(leido.costeEnvio()).contains(eur("7.95"));
        assertThat(leido.importeTotal()).contains(eur("167.63"));
    }

    @Test
    void givenPedidoConfirmado_whenGuardar_thenGuardaLosImportesEnSusColumnas() {
        // Detecta un cruce de parámetros BigDecimal en el constructor de PedidoEntity
        Pedido original = pedidoConfirmado(PedidoId.nuevo());

        adapter.guardar(original);

        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT importe_productos, coste_envio, importe_total FROM pedido WHERE id = ?",
                original.id().valor());
        assertThat((BigDecimal) fila.get("importe_productos")).isEqualByComparingTo("159.68");
        assertThat((BigDecimal) fila.get("coste_envio")).isEqualByComparingTo("7.95");
        assertThat((BigDecimal) fila.get("importe_total")).isEqualByComparingTo("167.63");
    }

    @Test
    void givenPedidoYaGuardado_whenGuardarloConfirmado_thenActualizaEstadoYCoste() {
        Pedido creado = pedidoCreado();
        adapter.guardar(creado);

        adapter.guardar(pedidoConfirmado(creado.id()));
        Pedido leido = adapter.buscarPorId(creado.id()).orElseThrow();

        assertThat(leido.estado()).isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(leido.costeEnvio()).contains(eur("7.95"));
        assertThat(leido.lineas()).hasSize(2);
    }

    // --- Búsqueda ---

    @Test
    void givenIdQueNoEstaEnBaseDeDatos_whenBuscarPorId_thenDevuelveVacio() {
        Optional<Pedido> resultado = adapter.buscarPorId(PedidoId.nuevo());

        assertThat(resultado).isEmpty();
    }
}
package com.perecollet.pedidos.domain.model;

import com.perecollet.pedidos.domain.exception.MonedaNoCoincideException;
import com.perecollet.pedidos.domain.exception.TransicionEstadoInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedidoTest {

    private static final ClienteId CLIENTE_ID = new ClienteId(UUID.randomUUID());
    private static final String CODIGO_POSTAL = "43840";
    private static final Instant FECHA = Instant.parse("2026-11-02T10:00:00Z");

    // --- Helpers ---

    private static Dinero eur(String importe) {
        return Dinero.euros(new BigDecimal(importe));
    }

    private static LineaPedido linea(String precio, String pesoKg, int cantidad) {
        Producto producto = new Producto(new ProductoId(UUID.randomUUID()), "Producto de prueba", eur(precio), new BigDecimal(pesoKg));
        return LineaPedido.of(producto, cantidad);
    }

    /** 19.99 x 2 = 39.98  +  5.50 x 3 = 16.50  →  56.48 EUR · peso 1.25x2 + 0.50x3 = 4.00 kg · 5 bultos */
    private static List<LineaPedido> lineasDeEjemplo() {
        return List.of(
                linea("19.99", "1.25", 2),
                linea("5.50", "0.50", 3)
        );
    }

    private static Pedido pedidoCreado() {
        return Pedido.crear(CLIENTE_ID, CODIGO_POSTAL, lineasDeEjemplo(), FECHA);
    }

    private static Pedido pedidoEnEstado(EstadoPedido estado) {
        return Pedido.reconstituir(PedidoId.nuevo(), CLIENTE_ID, CODIGO_POSTAL, lineasDeEjemplo(), FECHA, estado, null);
    }

    // --- Creación ---

    @Test
    void givenDatosValidos_whenCrear_thenPedidoEnEstadoCreadoConIdYFecha() {
        Pedido pedido = pedidoCreado();

        assertThat(pedido.id()).isNotNull();
        assertThat(pedido.clienteId()).isEqualTo(CLIENTE_ID);
        assertThat(pedido.codigoPostalDestino()).isEqualTo(CODIGO_POSTAL);
        assertThat(pedido.fecha()).isEqualTo(FECHA);
        assertThat(pedido.estado()).isEqualTo(EstadoPedido.CREADO);
    }

    @Test
    void givenDosPedidos_whenCrear_thenTienenIdsDistintos() {
        Pedido a = pedidoCreado();
        Pedido b = pedidoCreado();

        assertThat(a.id()).isNotEqualTo(b.id());
    }

    @Test
    void givenLineas_whenCrear_thenCalculaImporteDeProductos() {
        Pedido pedido = pedidoCreado();

        assertThat(pedido.importeProductos()).isEqualTo(eur("56.48"));
    }

    @Test
    void givenPedidoCreado_whenConsultarCosteEnvioYTotal_thenEstanVacios() {
        Pedido pedido = pedidoCreado();

        assertThat(pedido.costeEnvio()).isEmpty();
        assertThat(pedido.importeTotal()).isEmpty();
    }

    @Test
    void givenSinLineas_whenCrear_thenLanzaIllegalArgumentException() {
        List<LineaPedido> sinLineas = List.of();

        assertThatThrownBy(() -> Pedido.crear(CLIENTE_ID, CODIGO_POSTAL, sinLineas, FECHA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenLineaConPrecioEnOtraMoneda_whenCrear_thenLanzaMonedaNoCoincideException() {
        Dinero precioEnDolares = Dinero.of(new BigDecimal("10.00"), Currency.getInstance("USD"));
        Producto productoEnDolares = new Producto(new ProductoId(UUID.randomUUID()), "Producto en USD",
                precioEnDolares, new BigDecimal("1.00"));
        List<LineaPedido> lineas = List.of(linea("19.99", "1.25", 2), LineaPedido.of(productoEnDolares, 1));

        assertThatThrownBy(() -> Pedido.crear(CLIENTE_ID, CODIGO_POSTAL, lineas, FECHA))
                .isInstanceOf(MonedaNoCoincideException.class);
    }

    @Test
    void givenClienteNulo_whenCrear_thenLanzaNullPointerException() {
        List<LineaPedido> lineas = lineasDeEjemplo();

        assertThatThrownBy(() -> Pedido.crear(null, CODIGO_POSTAL, lineas, FECHA))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void givenListaOriginalModificadaDespuesDeCrear_whenConsultarLineas_thenPedidoNoCambia() {
        List<LineaPedido> lineas = new ArrayList<>(lineasDeEjemplo());
        Pedido pedido = Pedido.crear(CLIENTE_ID, CODIGO_POSTAL, lineas, FECHA);

        lineas.add(linea("100.00", "1.00", 1));

        assertThat(pedido.lineas()).hasSize(2);
        assertThat(pedido.importeProductos()).isEqualTo(eur("56.48"));
    }

    @Test
    void givenPedido_whenModificarLineasDesdeFuera_thenLanzaUnsupportedOperationException() {
        Pedido pedido = pedidoCreado();
        List<LineaPedido> lineas = pedido.lineas();
        LineaPedido nueva = linea("1.00", "1.00", 1);

        assertThatThrownBy(() -> lineas.add(nueva))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- Reconstitución (la usa el mapper de persistencia) ---

    @Test
    void givenEstadoConfirmadoConCoste_whenReconstituir_thenCalculaElTotal() {
        Pedido pedido = Pedido.reconstituir(PedidoId.nuevo(), CLIENTE_ID, CODIGO_POSTAL, lineasDeEjemplo(),
                FECHA, EstadoPedido.CONFIRMADO, eur("7.95"));

        assertThat(pedido.estado()).isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(pedido.costeEnvio()).contains(eur("7.95"));
        assertThat(pedido.importeTotal()).contains(eur("64.43"));
    }

    @Test
    void givenEstadoConfirmadoSinCoste_whenReconstituir_thenLanzaIllegalArgumentException() {
        PedidoId id = PedidoId.nuevo();
        List<LineaPedido> lineas = lineasDeEjemplo();

        assertThatThrownBy(() -> Pedido.reconstituir(id, CLIENTE_ID, CODIGO_POSTAL, lineas,
                FECHA, EstadoPedido.CONFIRMADO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenEstadoNoConfirmadoConCoste_whenReconstituir_thenLanzaIllegalArgumentException() {
        PedidoId id = PedidoId.nuevo();
        List<LineaPedido> lineas = lineasDeEjemplo();
        Dinero coste = eur("7.95");

        assertThatThrownBy(() -> Pedido.reconstituir(id, CLIENTE_ID, CODIGO_POSTAL, lineas,
                FECHA, EstadoPedido.CREADO, coste))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Datos para el proveedor logístico ---

    @Test
    void givenLineas_whenPesoTotalKg_thenSumaPesoPorCantidadDeCadaLinea() {
        Pedido pedido = pedidoCreado();

        assertThat(pedido.pesoTotalKg()).isEqualByComparingTo("4.00");
    }

    @Test
    void givenLineas_whenNumBultos_thenSumaLasCantidades() {
        Pedido pedido = pedidoCreado();

        assertThat(pedido.numBultos()).isEqualTo(5);
    }

    // --- Confirmación (test unitario del total que pide el enunciado) ---

    @Test
    void givenPedidoConfirmando_whenConfirmar_thenGuardaCosteEnvioYCalculaTotal() {
        Pedido pedido = pedidoEnEstado(EstadoPedido.CONFIRMANDO);

        pedido.confirmar(eur("7.95"));

        assertThat(pedido.estado()).isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(pedido.costeEnvio()).contains(eur("7.95"));
        assertThat(pedido.importeTotal()).contains(eur("64.43"));
    }

    @Test
    void givenPedidoConfirmando_whenConfirmarConOtraMoneda_thenLanzaExcepcionYNoCambiaElPedido() {
        Pedido pedido = pedidoEnEstado(EstadoPedido.CONFIRMANDO);
        Dinero costeEnDolares = Dinero.of(new BigDecimal("7.95"), Currency.getInstance("USD"));

        assertThatThrownBy(() -> pedido.confirmar(costeEnDolares))
                .isInstanceOf(MonedaNoCoincideException.class);

        assertThat(pedido.estado()).isEqualTo(EstadoPedido.CONFIRMANDO);
        assertThat(pedido.costeEnvio()).isEmpty();
        assertThat(pedido.importeTotal()).isEmpty();
    }

    @Test
    void givenPedidoCreado_whenConfirmar_thenLanzaTransicionEstadoInvalidaException() {
        Pedido pedido = pedidoCreado();
        Dinero coste = eur("7.95");

        assertThatThrownBy(() -> pedido.confirmar(coste))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void givenPedidoConfirmado_whenConfirmar_thenLanzaTransicionEstadoInvalidaException() {
        Pedido pedido = pedidoEnEstado(EstadoPedido.CONFIRMANDO);
        pedido.confirmar(eur("7.95"));
        Dinero otroCoste = eur("3.00");

        assertThatThrownBy(() -> pedido.confirmar(otroCoste))
                .isInstanceOf(TransicionEstadoInvalidaException.class);

        assertThat(pedido.costeEnvio()).contains(eur("7.95"));
    }

    @Test
    void givenPedidoErrorEnvio_whenConfirmar_thenLanzaTransicionEstadoInvalidaException() {
        Pedido pedido = pedidoEnEstado(EstadoPedido.ERROR_ENVIO);
        Dinero coste = eur("7.95");

        assertThatThrownBy(() -> pedido.confirmar(coste))
                .isInstanceOf(TransicionEstadoInvalidaException.class);

        assertThat(pedido.estado()).isEqualTo(EstadoPedido.ERROR_ENVIO);
        assertThat(pedido.costeEnvio()).isEmpty();
    }

    // --- Error de envío ---

    @Test
    void givenPedidoConfirmando_whenMarcarErrorEnvio_thenEstadoErrorEnvioSinCoste() {
        Pedido pedido = pedidoEnEstado(EstadoPedido.CONFIRMANDO);

        pedido.marcarErrorEnvio();

        assertThat(pedido.estado()).isEqualTo(EstadoPedido.ERROR_ENVIO);
        assertThat(pedido.costeEnvio()).isEmpty();
        assertThat(pedido.importeTotal()).isEmpty();
    }

    @Test
    void givenPedidoConfirmado_whenMarcarErrorEnvio_thenLanzaExcepcionYConservaElCoste() {
        Pedido pedido = Pedido.reconstituir(PedidoId.nuevo(), CLIENTE_ID, CODIGO_POSTAL, lineasDeEjemplo(),
                FECHA, EstadoPedido.CONFIRMADO, eur("7.95"));

        assertThatThrownBy(pedido::marcarErrorEnvio)
                .isInstanceOf(TransicionEstadoInvalidaException.class);

        assertThat(pedido.estado()).isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(pedido.costeEnvio()).contains(eur("7.95"));
    }

    @Test
    void givenPedidoCreado_whenMarcarErrorEnvio_thenLanzaTransicionEstadoInvalidaException() {
        Pedido pedido = pedidoCreado();

        assertThatThrownBy(pedido::marcarErrorEnvio)
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }
}
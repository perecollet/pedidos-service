package com.perecollet.pedidos.application.service;

import com.perecollet.pedidos.application.exception.PedidoEnConfirmacionException;
import com.perecollet.pedidos.application.exception.PedidoNoEncontradoException;
import com.perecollet.pedidos.application.exception.ProveedorLogisticoException;
import com.perecollet.pedidos.application.exception.ProveedorNoDisponibleException;
import com.perecollet.pedidos.application.port.out.PedidoRepositoryPort;
import com.perecollet.pedidos.application.port.out.ProveedorLogisticoPort;
import com.perecollet.pedidos.domain.exception.MonedaNoCoincideException;
import com.perecollet.pedidos.domain.model.ClienteId;
import com.perecollet.pedidos.domain.model.Dinero;
import com.perecollet.pedidos.domain.model.EstadoPedido;
import com.perecollet.pedidos.domain.model.LineaPedido;
import com.perecollet.pedidos.domain.model.Pedido;
import com.perecollet.pedidos.domain.model.PedidoId;
import com.perecollet.pedidos.domain.model.Producto;
import com.perecollet.pedidos.domain.model.ProductoId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmarPedidoServiceTest {

    private static final PedidoId PEDIDO_ID = PedidoId.nuevo();
    private static final ClienteId CLIENTE_ID = new ClienteId(UUID.randomUUID());
    private static final String CODIGO_POSTAL = "43840";
    private static final Instant FECHA = Instant.parse("2026-11-05T10:00:00Z");

    @Mock
    private PedidoRepositoryPort pedidos;
    @Mock
    private ProveedorLogisticoPort proveedor;

    private ConfirmarPedidoService servicio;

    @BeforeEach
    void setUp() {
        servicio = new ConfirmarPedidoService(pedidos, proveedor);
    }

    // --- Helpers ---

    private static Dinero eur(String importe) {
        return Dinero.euros(new BigDecimal(importe));
    }

    private static LineaPedido linea(String precio, String pesoKg, int cantidad) {
        Producto producto = new Producto(new ProductoId(UUID.randomUUID()), "Producto de prueba",
                eur(precio), new BigDecimal(pesoKg));
        return LineaPedido.of(producto, cantidad);
    }

    /** 19.99 x 2 + 5.50 x 3 = 56.48 EUR · peso 1.25x2 + 0.50x3 = 4.00 kg · 5 bultos */
    private static Pedido pedidoEnEstado(EstadoPedido estado) {
        List<LineaPedido> lineas = List.of(linea("19.99", "1.25", 2), linea("5.50", "0.50", 3));
        Dinero coste = estado == EstadoPedido.CONFIRMADO ? eur("7.95") : null;
        return Pedido.reconstituir(PEDIDO_ID, CLIENTE_ID, CODIGO_POSTAL, lineas, FECHA, estado, coste);
    }

    /** La reserva funciona y, al leerlo, el pedido ya está en CONFIRMANDO. */
    private void dadoPedidoReservado() {
        when(pedidos.reservarParaConfirmacion(PEDIDO_ID)).thenReturn(true);
        when(pedidos.buscarPorId(PEDIDO_ID)).thenReturn(Optional.of(pedidoEnEstado(EstadoPedido.CONFIRMANDO)));
    }

    /** La reserva no actualiza ninguna fila y el pedido está en el estado indicado. */
    private void dadoReservaRechazadaConPedidoEn(EstadoPedido estado) {
        when(pedidos.reservarParaConfirmacion(PEDIDO_ID)).thenReturn(false);
        when(pedidos.buscarPorId(PEDIDO_ID)).thenReturn(Optional.of(pedidoEnEstado(estado)));
    }

    private Pedido pedidoGuardado() {
        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidos).guardar(captor.capture());
        return captor.getValue();
    }

    // --- Camino feliz ---

    @Test
    void givenPedidoReservado_whenConfirmar_thenPideTarifaConLosDatosDelPedido() {
        dadoPedidoReservado();
        when(proveedor.obtenerTarifa(anyString(), any(), anyInt())).thenReturn(eur("7.95"));

        servicio.confirmar(PEDIDO_ID);

        ArgumentCaptor<BigDecimal> peso = ArgumentCaptor.forClass(BigDecimal.class);
        verify(proveedor).obtenerTarifa(eq(CODIGO_POSTAL), peso.capture(), eq(5));
        assertThat(peso.getValue()).isEqualByComparingTo("4.00");
    }

    @Test
    void givenPedidoReservado_whenConfirmar_thenGuardaYDevuelvePedidoConfirmadoConTotal() {
        dadoPedidoReservado();
        when(proveedor.obtenerTarifa(anyString(), any(), anyInt())).thenReturn(eur("7.95"));

        Pedido devuelto = servicio.confirmar(PEDIDO_ID);

        Pedido guardado = pedidoGuardado();
        assertThat(guardado.estado()).isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(guardado.costeEnvio()).contains(eur("7.95"));
        assertThat(guardado.importeTotal()).contains(eur("64.43"));
        assertThat(devuelto).isSameAs(guardado);
    }

    @Test
    void givenPedidoReservado_whenConfirmar_thenReservaAntesDeLlamarAlProveedor() {
        dadoPedidoReservado();
        when(proveedor.obtenerTarifa(anyString(), any(), anyInt())).thenReturn(eur("7.95"));

        servicio.confirmar(PEDIDO_ID);

        InOrder orden = inOrder(pedidos, proveedor);
        orden.verify(pedidos).reservarParaConfirmacion(PEDIDO_ID);
        orden.verify(proveedor).obtenerTarifa(anyString(), any(), anyInt());
        orden.verify(pedidos).guardar(any());
    }

    // --- Reserva rechazada (otra confirmación ganó o el pedido no admite confirmación) ---

    @Test
    void givenPedidoYaConfirmado_whenConfirmar_thenDevuelveElPedidoSinLlamarAlProveedor() {
        dadoReservaRechazadaConPedidoEn(EstadoPedido.CONFIRMADO);

        Pedido devuelto = servicio.confirmar(PEDIDO_ID);

        assertThat(devuelto.estado()).isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(devuelto.importeTotal()).contains(eur("64.43"));
        verifyNoInteractions(proveedor);
        verify(pedidos, never()).guardar(any());
    }

    @Test
    void givenPedidoEnConfirmacion_whenConfirmar_thenLanzaPedidoEnConfirmacionSinLlamarAlProveedor() {
        dadoReservaRechazadaConPedidoEn(EstadoPedido.CONFIRMANDO);

        assertThatThrownBy(() -> servicio.confirmar(PEDIDO_ID))
                .isInstanceOf(PedidoEnConfirmacionException.class)
                .hasMessageContaining(PEDIDO_ID.valor().toString());

        verifyNoInteractions(proveedor);
        verify(pedidos, never()).guardar(any());
    }

    @Test
    void givenPedidoInexistente_whenConfirmar_thenLanzaPedidoNoEncontrado() {
        when(pedidos.reservarParaConfirmacion(PEDIDO_ID)).thenReturn(false);
        when(pedidos.buscarPorId(PEDIDO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.confirmar(PEDIDO_ID))
                .isInstanceOf(PedidoNoEncontradoException.class)
                .hasMessageContaining(PEDIDO_ID.valor().toString());

        verifyNoInteractions(proveedor);
    }

    // --- Fallos del proveedor: el pedido queda en ERROR_ENVIO y la excepción se propaga ---

    @Test
    void givenProveedorNoDisponible_whenConfirmar_thenGuardaErrorEnvioYPropagaLaExcepcion() {
        dadoPedidoReservado();
        ProveedorNoDisponibleException fallo = new ProveedorNoDisponibleException("503 del proveedor");
        when(proveedor.obtenerTarifa(anyString(), any(), anyInt())).thenThrow(fallo);

        assertThatThrownBy(() -> servicio.confirmar(PEDIDO_ID)).isSameAs(fallo);

        Pedido guardado = pedidoGuardado();
        assertThat(guardado.estado()).isEqualTo(EstadoPedido.ERROR_ENVIO);
        assertThat(guardado.costeEnvio()).isEmpty();
    }

    @Test
    void givenFalloNoReintentableDelProveedor_whenConfirmar_thenGuardaErrorEnvioYPropagaLaExcepcion() {
        dadoPedidoReservado();
        ProveedorLogisticoException fallo = new ProveedorLogisticoException("400 del proveedor");
        when(proveedor.obtenerTarifa(anyString(), any(), anyInt())).thenThrow(fallo);

        assertThatThrownBy(() -> servicio.confirmar(PEDIDO_ID)).isSameAs(fallo);

        assertThat(pedidoGuardado().estado()).isEqualTo(EstadoPedido.ERROR_ENVIO);
    }

    @Test
    void givenTarifaEnOtraMoneda_whenConfirmar_thenGuardaErrorEnvioYLanzaMonedaNoCoincide() {
        dadoPedidoReservado();
        Dinero tarifaEnDolares = Dinero.of(new BigDecimal("7.95"), Currency.getInstance("USD"));
        when(proveedor.obtenerTarifa(anyString(), any(), anyInt())).thenReturn(tarifaEnDolares);

        assertThatThrownBy(() -> servicio.confirmar(PEDIDO_ID))
                .isInstanceOf(MonedaNoCoincideException.class);

        Pedido guardado = pedidoGuardado();
        assertThat(guardado.estado()).isEqualTo(EstadoPedido.ERROR_ENVIO);
        assertThat(guardado.costeEnvio()).isEmpty();
    }
}
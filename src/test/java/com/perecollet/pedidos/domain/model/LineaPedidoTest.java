package com.perecollet.pedidos.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LineaPedidoTest {

    private static Dinero eur(String importe) {
        return Dinero.euros(new BigDecimal(importe));
    }

    private static Producto producto(String precio, String pesoKg) {
        return new Producto(new ProductoId(UUID.randomUUID()), "Producto de prueba", eur(precio), new BigDecimal(pesoKg));
    }

    @Test
    void givenProductoYCantidad_whenOf_thenCopiaProductoPrecioYPeso() {
        Producto producto = producto("19.99", "1.25");

        LineaPedido linea = LineaPedido.of(producto, 2);

        assertThat(linea.productoId()).isEqualTo(producto.id());
        assertThat(linea.cantidad()).isEqualTo(2);
        assertThat(linea.precioUnitario()).isEqualTo(eur("19.99"));
        assertThat(linea.pesoKg()).isEqualByComparingTo("1.25");
    }

    @Test
    void givenLinea_whenImporte_thenDevuelvePrecioPorCantidad() {
        LineaPedido linea = LineaPedido.of(producto("19.99", "1.25"), 3);

        assertThat(linea.importe()).isEqualTo(eur("59.97"));
    }

    @Test
    void givenLinea_whenPesoTotalKg_thenDevuelvePesoPorCantidad() {
        LineaPedido linea = LineaPedido.of(producto("19.99", "1.25"), 3);

        assertThat(linea.pesoTotalKg()).isEqualByComparingTo("3.75");
    }

    @Test
    void givenCantidadCero_whenOf_thenLanzaIllegalArgumentException() {
        Producto producto = producto("19.99", "1.25");

        assertThatThrownBy(() -> LineaPedido.of(producto, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenCantidadNegativa_whenOf_thenLanzaIllegalArgumentException() {
        Producto producto = producto("19.99", "1.25");

        assertThatThrownBy(() -> LineaPedido.of(producto, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenProductoNulo_whenOf_thenLanzaNullPointerException() {
        assertThatThrownBy(() -> LineaPedido.of(null, 1))
                .isInstanceOf(NullPointerException.class);
    }

    // --- Invariantes del constructor (lo usa el mapper de persistencia, sin pasar por of) ---

    private static final ProductoId PRODUCTO_ID = new ProductoId(UUID.randomUUID());
    private static final Dinero PRECIO = eur("19.99");
    private static final BigDecimal PESO = new BigDecimal("1.25");

    @Test
    void givenProductoIdNulo_whenConstruir_thenLanzaNullPointerException() {
        assertThatThrownBy(() -> new LineaPedido(null, 1, PRECIO, PESO))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void givenPrecioUnitarioNulo_whenConstruir_thenLanzaNullPointerException() {
        assertThatThrownBy(() -> new LineaPedido(PRODUCTO_ID, 1, null, PESO))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void givenPesoNulo_whenConstruir_thenLanzaNullPointerException() {
        assertThatThrownBy(() -> new LineaPedido(PRODUCTO_ID, 1, PRECIO, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void givenPesoNegativo_whenConstruir_thenLanzaIllegalArgumentException() {
        BigDecimal pesoNegativo = new BigDecimal("-0.01");

        assertThatThrownBy(() -> new LineaPedido(PRODUCTO_ID, 1, PRECIO, pesoNegativo))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenCantidadCero_whenConstruir_thenLanzaIllegalArgumentException() {
        assertThatThrownBy(() -> new LineaPedido(PRODUCTO_ID, 0, PRECIO, PESO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
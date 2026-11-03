package com.perecollet.pedidos.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductoTest {

    private static final ProductoId ID = new ProductoId(UUID.randomUUID());
    private static final String NOMBRE = "Teclado mecánico";
    private static final Dinero PRECIO = Dinero.euros(new BigDecimal("49.99"));
    private static final BigDecimal PESO = new BigDecimal("1.25");

    @Test
    void givenDatosValidos_whenCrear_thenGuardaTodosLosCampos() {
        Producto producto = new Producto(ID, NOMBRE, PRECIO, PESO);

        assertThat(producto.id()).isEqualTo(ID);
        assertThat(producto.nombre()).isEqualTo(NOMBRE);
        assertThat(producto.precio()).isEqualTo(PRECIO);
        assertThat(producto.pesoKg()).isEqualByComparingTo("1.25");
    }

    @Test
    void givenPesoCero_whenCrear_thenSePermite() {
        BigDecimal pesoCero = BigDecimal.ZERO;

        Producto producto = new Producto(ID, NOMBRE, PRECIO, pesoCero);

        assertThat(producto.pesoKg()).isEqualByComparingTo("0");
    }

    @Test
    void givenPesoNegativo_whenCrear_thenLanzaIllegalArgumentException() {
        BigDecimal pesoNegativo = new BigDecimal("-0.01");

        assertThatThrownBy(() -> new Producto(ID, NOMBRE, PRECIO, pesoNegativo))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenIdNulo_whenCrear_thenLanzaNullPointerException() {
        assertThatThrownBy(() -> new Producto(null, NOMBRE, PRECIO, PESO))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void givenNombreNulo_whenCrear_thenLanzaNullPointerException() {
        assertThatThrownBy(() -> new Producto(ID, null, PRECIO, PESO))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void givenPrecioNulo_whenCrear_thenLanzaNullPointerException() {
        assertThatThrownBy(() -> new Producto(ID, NOMBRE, null, PESO))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void givenPesoNulo_whenCrear_thenLanzaNullPointerException() {
        assertThatThrownBy(() -> new Producto(ID, NOMBRE, PRECIO, null))
                .isInstanceOf(NullPointerException.class);
    }
}
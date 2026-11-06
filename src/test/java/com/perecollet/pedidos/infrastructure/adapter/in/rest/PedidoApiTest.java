package com.perecollet.pedidos.infrastructure.adapter.in.rest;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.perecollet.pedidos.TestcontainersConfiguration;
import com.perecollet.pedidos.application.port.out.PedidoRepositoryPort;
import com.perecollet.pedidos.domain.model.EstadoPedido;
import com.perecollet.pedidos.domain.model.PedidoId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class PedidoApiTest {

    // Ids fijos de V2__seed.sql
    private static final String CLIENTE_ANA = "11111111-1111-1111-1111-111111111111";
    private static final String TECLADO = "22222222-2222-2222-2222-222222222221";   // 49.99 EUR · 1.250 kg
    private static final String RATON = "22222222-2222-2222-2222-222222222222";     // 19.90 EUR · 0.200 kg

    private static final String TARIFAS = "/api/tarifas";
    private static final String TARIFA_OK = """
            { "coste": 7.95, "moneda": "EUR", "plazoDias": 2 }
            """;

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() { };

    @RegisterExtension
    static WireMockExtension proveedor = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("proveedor-logistico.base-url", proveedor::baseUrl);
        registro.add("proveedor-logistico.token", () -> "token-de-prueba");
        registro.add("proveedor-logistico.read-timeout", () -> "1s");
        registro.add("proveedor-logistico.reintentos.espera", () -> "10ms");
        registro.add("proveedor-logistico.reintentos.jitter", () -> "0ms");
    }

    @Value("${local.server.port}")
    private int puerto;

    @Autowired
    private PedidoRepositoryPort pedidos;

    private RestClient api;

    @BeforeEach
    void setUp() {
        api = RestClient.builder()
                .baseUrl("http://localhost:" + puerto)
                // No lanzar excepciones con 4xx/5xx: los tests comprueban el código de estado
                .defaultStatusHandler(estado -> true, (peticion, respuesta) -> { })
                .build();
    }

    // --- Helpers ---

    /** Teclado x2 (99.98) + Ratón x3 (59.70) = 159.68 EUR · peso 2.50 + 0.60 = 3.10 kg · 5 bultos */
    private static String pedidoValido() {
        return """
                {
                  "clienteId": "%s",
                  "codigoPostalDestino": "43840",
                  "lineas": [
                    { "productoId": "%s", "cantidad": 2 },
                    { "productoId": "%s", "cantidad": 3 }
                  ]
                }
                """.formatted(CLIENTE_ANA, TECLADO, RATON);
    }

    private ResponseEntity<Map<String, Object>> crear(String cuerpo) {
        return api.post().uri("/api/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .toEntity(JSON);
    }

    private String crearPedidoValido() {
        return (String) crear(pedidoValido()).getBody().get("id");
    }

    private ResponseEntity<Map<String, Object>> confirmar(String id) {
        return api.post().uri("/api/pedidos/{id}/confirmacion", id)
                .retrieve()
                .toEntity(JSON);
    }

    private EstadoPedido estadoGuardado(String id) {
        return pedidos.buscarPorId(new PedidoId(UUID.fromString(id))).orElseThrow().estado();
    }

    private static BigDecimal importe(Map<String, Object> cuerpo, String campo) {
        return new BigDecimal(cuerpo.get(campo).toString());
    }

    private void verificarLlamadasAlProveedor(int numero) {
        proveedor.verify(exactly(numero), postRequestedFor(urlEqualTo(TARIFAS)));
    }

    // --- POST /api/pedidos ---

    @Test
    void givenPedidoValido_whenCrear_thenDevuelve201ConImporteCalculado() {
        ResponseEntity<Map<String, Object>> respuesta = crear(pedidoValido());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> cuerpo = respuesta.getBody();
        assertThat(cuerpo.get("estado")).isEqualTo("CREADO");
        assertThat(importe(cuerpo, "importeProductos")).isEqualByComparingTo("159.68");
        assertThat(cuerpo.get("costeEnvio")).isNull();
        assertThat(cuerpo.get("importeTotal")).isNull();
    }

    @Test
    void givenImportesEnviadosPorElCliente_whenCrear_thenSeIgnoranYSeCalculanEnElServidor() {
        String conImportesFalsos = pedidoValido().replaceFirst("\\{",
                "{ \"importeProductos\": 0.01, \"importeTotal\": 0.01, \"estado\": \"CONFIRMADO\",");

        ResponseEntity<Map<String, Object>> respuesta = crear(conImportesFalsos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(importe(respuesta.getBody(), "importeProductos")).isEqualByComparingTo("159.68");
        assertThat(respuesta.getBody().get("estado")).isEqualTo("CREADO");
    }

    @Test
    void givenClienteInexistente_whenCrear_thenDevuelve422() {
        String cuerpo = pedidoValido().replace(CLIENTE_ANA, UUID.randomUUID().toString());

        assertThat(crear(cuerpo).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void givenProductoInexistente_whenCrear_thenDevuelve422() {
        String cuerpo = pedidoValido().replace(RATON, UUID.randomUUID().toString());

        assertThat(crear(cuerpo).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void givenPedidoSinLineas_whenCrear_thenDevuelve400() {
        String cuerpo = """
                { "clienteId": "%s", "codigoPostalDestino": "43840", "lineas": [] }
                """.formatted(CLIENTE_ANA);

        assertThat(crear(cuerpo).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void givenCantidadCero_whenCrear_thenDevuelve400() {
        String cuerpo = pedidoValido().replace("\"cantidad\": 2", "\"cantidad\": 0");

        assertThat(crear(cuerpo).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4384", "438400", "12345678901", "4384a", "@@@@@", "     "})
    void givenCodigoPostalQueNoTieneCincoDigitos_whenCrear_thenDevuelve400(String codigoPostal) {
        String cuerpo = pedidoValido().replace("43840", codigoPostal);

        assertThat(crear(cuerpo).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void givenCodigoPostalNulo_whenCrear_thenDevuelve400() {
        String cuerpo = pedidoValido().replace("\"43840\"", "null");

        assertThat(crear(cuerpo).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- POST /api/pedidos/{id}/confirmacion ---

    @Test
    void givenProveedorResponde_whenConfirmar_thenDevuelve200ConCosteYTotal() {
        proveedor.stubFor(post(TARIFAS).willReturn(okJson(TARIFA_OK)));
        String id = crearPedidoValido();

        ResponseEntity<Map<String, Object>> respuesta = confirmar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> cuerpo = respuesta.getBody();
        assertThat(cuerpo.get("estado")).isEqualTo("CONFIRMADO");
        assertThat(importe(cuerpo, "costeEnvio")).isEqualByComparingTo("7.95");
        assertThat(importe(cuerpo, "importeTotal")).isEqualByComparingTo("167.63");
        proveedor.verify(postRequestedFor(urlEqualTo(TARIFAS)).withRequestBody(equalToJson("""
                { "codigoPostalDestino": "43840", "pesoKg": 3.10, "numBultos": 5 }
                """)));
    }

    @Test
    void givenPedidoYaConfirmado_whenConfirmarOtraVez_thenDevuelve200SinVolverALlamarAlProveedor() {
        proveedor.stubFor(post(TARIFAS).willReturn(okJson(TARIFA_OK)));
        String id = crearPedidoValido();
        confirmar(id);

        ResponseEntity<Map<String, Object>> segunda = confirmar(id);

        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(importe(segunda.getBody(), "importeTotal")).isEqualByComparingTo("167.63");
        verificarLlamadasAlProveedor(1);
    }

    @Test
    void givenPedidoInexistente_whenConfirmar_thenDevuelve404() {
        assertThat(confirmar(UUID.randomUUID().toString()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- Fallos del proveedor (test de integración que pide el enunciado) ---

    @Test
    void givenProveedorNoRespondeATiempo_whenConfirmar_thenDevuelve504YPedidoQuedaEnErrorEnvio() {
        proveedor.stubFor(post(TARIFAS).willReturn(okJson(TARIFA_OK).withFixedDelay(2_000)));
        String id = crearPedidoValido();

        ResponseEntity<Map<String, Object>> respuesta = confirmar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(estadoGuardado(id)).isEqualTo(EstadoPedido.ERROR_ENVIO);
        verificarLlamadasAlProveedor(3);
    }

    @Test
    void givenProveedorSiempreResponde503_whenConfirmar_thenDevuelve503YPedidoQuedaEnErrorEnvio() {
        proveedor.stubFor(post(TARIFAS).willReturn(serviceUnavailable()));
        String id = crearPedidoValido();

        ResponseEntity<Map<String, Object>> respuesta = confirmar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(estadoGuardado(id)).isEqualTo(EstadoPedido.ERROR_ENVIO);
    }

    @Test
    void givenPedidoEnErrorEnvio_whenConfirmarDeNuevoYProveedorResponde_thenSeConfirma() {
        proveedor.stubFor(post(TARIFAS).willReturn(serviceUnavailable()));
        String id = crearPedidoValido();
        confirmar(id);
        proveedor.resetAll();
        proveedor.stubFor(post(TARIFAS).willReturn(okJson(TARIFA_OK)));

        ResponseEntity<Map<String, Object>> reintento = confirmar(id);

        assertThat(reintento.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reintento.getBody().get("estado")).isEqualTo("CONFIRMADO");
    }

    // --- Respuestas anómalas: el pedido no se queda bloqueado en CONFIRMANDO ---

    @Test
    void givenCosteQueNoCabeEnLaColumna_whenConfirmar_thenDevuelve502YSePuedeReintentar() {
        proveedor.stubFor(post(TARIFAS).willReturn(okJson("""
                { "coste": 99999999999.00, "moneda": "EUR", "plazoDias": 2 }
                """)));
        String id = crearPedidoValido();

        ResponseEntity<Map<String, Object>> respuesta = confirmar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(estadoGuardado(id)).isEqualTo(EstadoPedido.ERROR_ENVIO);
        proveedor.resetAll();
        proveedor.stubFor(post(TARIFAS).willReturn(okJson(TARIFA_OK)));
        assertThat(confirmar(id).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void givenTotalQueNoCabeEnLaColumna_whenConfirmar_thenDevuelve500YSePuedeReintentar() {
        // El coste es válido, pero 9999999999.99 + 159.68 desborda importe_total y falla el guardado
        proveedor.stubFor(post(TARIFAS).willReturn(okJson("""
                { "coste": 9999999999.99, "moneda": "EUR", "plazoDias": 2 }
                """)));
        String id = crearPedidoValido();

        ResponseEntity<Map<String, Object>> respuesta = confirmar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(estadoGuardado(id)).isEqualTo(EstadoPedido.ERROR_ENVIO);
        proveedor.resetAll();
        proveedor.stubFor(post(TARIFAS).willReturn(okJson(TARIFA_OK)));
        assertThat(confirmar(id).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- Concurrencia: dos confirmaciones simultáneas del mismo pedido ---

    @Test
    void givenDosConfirmacionesSimultaneas_whenConfirmar_thenUnaLlamadaAlProveedorYUnConflicto() throws Exception {
        // El proveedor tarda 300 ms: la segunda petición llega mientras la primera está esperando
        proveedor.stubFor(post(TARIFAS).willReturn(okJson(TARIFA_OK).withFixedDelay(300)));
        String id = crearPedidoValido();
        CountDownLatch salida = new CountDownLatch(1);
        Callable<HttpStatus> confirmacion = () -> {
            salida.await();
            return HttpStatus.valueOf(confirmar(id).getStatusCode().value());
        };

        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<HttpStatus> primera = hilos.submit(confirmacion);
            Future<HttpStatus> segunda = hilos.submit(confirmacion);
            salida.countDown();

            List<HttpStatus> estados = List.of(primera.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS));

            assertThat(estados).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);
        } finally {
            hilos.shutdownNow();
        }
        verificarLlamadasAlProveedor(1);
        assertThat(estadoGuardado(id)).isEqualTo(EstadoPedido.CONFIRMADO);
    }
}
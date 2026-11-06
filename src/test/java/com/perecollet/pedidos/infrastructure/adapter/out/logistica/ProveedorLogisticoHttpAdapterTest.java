package com.perecollet.pedidos.infrastructure.adapter.out.logistica;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.perecollet.pedidos.TestcontainersConfiguration;
import com.perecollet.pedidos.application.exception.ProveedorLogisticoException;
import com.perecollet.pedidos.application.exception.ProveedorNoDisponibleException;
import com.perecollet.pedidos.application.exception.ProveedorTimeoutException;
import com.perecollet.pedidos.application.port.out.ProveedorLogisticoPort;
import com.perecollet.pedidos.domain.model.Dinero;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Currency;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProveedorLogisticoHttpAdapterTest {

    private static final String TARIFAS = "/api/tarifas";
    private static final String TOKEN = "token-de-prueba";
    private static final String RESPUESTA_OK = """
            { "coste": 7.95, "moneda": "EUR", "plazoDias": 2 }
            """;

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("proveedor-logistico.base-url", wiremock::baseUrl);
        registro.add("proveedor-logistico.token", () -> TOKEN);
        registro.add("proveedor-logistico.connect-timeout", () -> "500ms");
        registro.add("proveedor-logistico.read-timeout", () -> "500ms");
        registro.add("proveedor-logistico.reintentos.espera", () -> "10ms");
        registro.add("proveedor-logistico.reintentos.jitter", () -> "0ms");
    }

    @Autowired
    private ProveedorLogisticoPort proveedor;

    private Dinero pedirTarifa() {
        return proveedor.obtenerTarifa("43840", new BigDecimal("3.5"), 2);
    }

    private void verificarLlamadas(int numero) {
        wiremock.verify(exactly(numero), postRequestedFor(urlEqualTo(TARIFAS)));
    }

    // --- Respuesta correcta ---

    @Test
    void givenProveedorResponde200_whenObtenerTarifa_thenDevuelveElCoste() {
        wiremock.stubFor(post(TARIFAS).willReturn(okJson(RESPUESTA_OK)));

        Dinero tarifa = pedirTarifa();

        assertThat(tarifa).isEqualTo(Dinero.euros(new BigDecimal("7.95")));
    }

    @Test
    void givenProveedorResponde200_whenObtenerTarifa_thenEnviaTokenYCuerpoSegunElContrato() {
        wiremock.stubFor(post(TARIFAS).willReturn(okJson(RESPUESTA_OK)));

        pedirTarifa();

        wiremock.verify(postRequestedFor(urlEqualTo(TARIFAS))
                .withHeader("Authorization", equalTo("Bearer " + TOKEN))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("""
                        { "codigoPostalDestino": "43840", "pesoKg": 3.5, "numBultos": 2 }
                        """)));
    }

    @Test
    void givenProveedorRespondeEnOtraMoneda_whenObtenerTarifa_thenDevuelveLaMonedaRecibida() {
        // El adaptador traduce, no decide: rechazar monedas distintas de EUR es cosa del dominio
        wiremock.stubFor(post(TARIFAS).willReturn(okJson("""
                { "coste": 7.95, "moneda": "USD", "plazoDias": 2 }
                """)));

        Dinero tarifa = pedirTarifa();

        assertThat(tarifa.moneda()).isEqualTo(Currency.getInstance("USD"));
    }

    // --- Fallos temporales: se reintentan (3 intentos en total) ---

    @Test
    void givenProveedorResponde503YLuego200_whenObtenerTarifa_thenReintentaYDevuelveElCoste() {
        wiremock.stubFor(post(TARIFAS).inScenario("intermitente").whenScenarioStateIs(STARTED)
                .willReturn(serviceUnavailable())
                .willSetStateTo("recuperado"));
        wiremock.stubFor(post(TARIFAS).inScenario("intermitente").whenScenarioStateIs("recuperado")
                .willReturn(okJson(RESPUESTA_OK)));

        Dinero tarifa = pedirTarifa();

        assertThat(tarifa).isEqualTo(Dinero.euros(new BigDecimal("7.95")));
        verificarLlamadas(2);
    }

    @Test
    void givenProveedorSiempreResponde503_whenObtenerTarifa_thenReintentaYLanzaNoDisponible() {
        wiremock.stubFor(post(TARIFAS).willReturn(serviceUnavailable()));

        assertThatThrownBy(this::pedirTarifa)
                .isExactlyInstanceOf(ProveedorNoDisponibleException.class);

        verificarLlamadas(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 502, 504})
    void givenProveedorRespondeEstadoTemporal_whenObtenerTarifa_thenReintentaYLanzaNoDisponible(int estado) {
        wiremock.stubFor(post(TARIFAS).willReturn(aResponse().withStatus(estado)));

        assertThatThrownBy(this::pedirTarifa)
                .isExactlyInstanceOf(ProveedorNoDisponibleException.class);

        verificarLlamadas(3);
    }

    @Test
    void givenProveedorTardaMasQueElTimeout_whenObtenerTarifa_thenReintentaYLanzaTimeout() {
        wiremock.stubFor(post(TARIFAS).willReturn(okJson(RESPUESTA_OK).withFixedDelay(1_500)));

        assertThatThrownBy(this::pedirTarifa)
                .isExactlyInstanceOf(ProveedorTimeoutException.class);

        verificarLlamadas(3);
    }

    @Test
    void givenCuerpoQueTardaMasQueElTimeout_whenObtenerTarifa_thenReintentaYLanzaNoDisponible() {
        // Las cabeceras llegan a tiempo y se atasca el cuerpo: el cliente lo notifica como IOException genérica
        // ("subscription cancelled"), no como HttpTimeoutException, así que no se distingue de un corte de conexión
        wiremock.stubFor(post(TARIFAS).willReturn(okJson(RESPUESTA_OK).withChunkedDribbleDelay(8, 2_000)));

        assertThatThrownBy(this::pedirTarifa)
                .isExactlyInstanceOf(ProveedorNoDisponibleException.class);

        verificarLlamadas(3);
    }

    @Test
    void givenConexionCortadaLeyendoElCuerpo_whenObtenerTarifa_thenLanzaNoDisponible() throws IOException {
        // WireMock no permite cortar a mitad de cuerpo: se anuncian 1000 bytes y se cierra tras enviar unos pocos
        HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext(TARIFAS, intercambio -> {
            intercambio.getRequestBody().readAllBytes();
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, 1_000);
            intercambio.getResponseBody().write("{ \"coste\":".getBytes(StandardCharsets.UTF_8));
            intercambio.getResponseBody().flush();
            intercambio.close();
        });
        servidor.start();
        try {
            ProveedorLogisticoHttpAdapter sinReintentos = new ProveedorLogisticoHttpAdapter(RestClient.builder()
                    .baseUrl("http://localhost:" + servidor.getAddress().getPort())
                    .requestFactory(new JdkClientHttpRequestFactory())
                    .build());
            BigDecimal peso = new BigDecimal("3.5");

            assertThatThrownBy(() -> sinReintentos.obtenerTarifa("43840", peso, 2))
                    .isExactlyInstanceOf(ProveedorNoDisponibleException.class);
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void givenConexionCortada_whenObtenerTarifa_thenReintentaYLanzaNoDisponible() {
        wiremock.stubFor(post(TARIFAS).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(this::pedirTarifa)
                .isExactlyInstanceOf(ProveedorNoDisponibleException.class);

        verificarLlamadas(3);
    }

    // --- Fallos no temporales: NO se reintentan ---

    @Test
    void givenProveedorResponde400_whenObtenerTarifa_thenLanzaErrorSinReintentar() {
        wiremock.stubFor(post(TARIFAS).willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(this::pedirTarifa)
                .isExactlyInstanceOf(ProveedorLogisticoException.class);

        verificarLlamadas(1);
    }

    @Test
    void givenProveedorResponde401_whenObtenerTarifa_thenLanzaErrorSinReintentar() {
        // Token inválido o caducado: reintentar no lo arregla
        wiremock.stubFor(post(TARIFAS).willReturn(aResponse().withStatus(401)));

        assertThatThrownBy(this::pedirTarifa)
                .isExactlyInstanceOf(ProveedorLogisticoException.class);

        verificarLlamadas(1);
    }

    @Test
    void givenProveedorResponde500_whenObtenerTarifa_thenLanzaErrorSinReintentar() {
        wiremock.stubFor(post(TARIFAS).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(this::pedirTarifa)
                .isExactlyInstanceOf(ProveedorLogisticoException.class);

        verificarLlamadas(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10000000000.00", "1E+2147483647", "1E-2147483647"})
    void givenCosteNoRepresentable_whenObtenerTarifa_thenLanzaErrorSinReintentar(String coste) {
        wiremock.stubFor(post(TARIFAS).willReturn(okJson("""
                { "coste": %s, "moneda": "EUR", "plazoDias": 2 }
                """.formatted(coste))));

        assertThatThrownBy(this::pedirTarifa)
                .isExactlyInstanceOf(ProveedorLogisticoException.class);

        verificarLlamadas(1);
    }

    @Test
    void givenCosteEnElLimiteDeLaColumna_whenObtenerTarifa_thenDevuelveElCoste() {
        wiremock.stubFor(post(TARIFAS).willReturn(okJson("""
                { "coste": 9999999999.99, "moneda": "EUR", "plazoDias": 2 }
                """)));

        Dinero tarifa = pedirTarifa();

        assertThat(tarifa).isEqualTo(Dinero.euros(new BigDecimal("9999999999.99")));
    }

    @Test
    void givenJsonRoto_whenObtenerTarifa_thenLanzaErrorSinReintentar() {
        // Un error de parseo no es un fallo de red, aunque se detecte al leer el cuerpo
        wiremock.stubFor(post(TARIFAS).willReturn(okJson("{ \"coste\": ")));

        assertThatThrownBy(this::pedirTarifa)
                .isExactlyInstanceOf(ProveedorLogisticoException.class);

        verificarLlamadas(1);
    }

    @Test
    void givenRespuestaSinCoste_whenObtenerTarifa_thenLanzaErrorSinReintentar() {
        wiremock.stubFor(post(TARIFAS).willReturn(okJson("""
                { "moneda": "EUR", "plazoDias": 2 }
                """)));

        assertThatThrownBy(this::pedirTarifa)
                .isExactlyInstanceOf(ProveedorLogisticoException.class);

        verificarLlamadas(1);
    }
}
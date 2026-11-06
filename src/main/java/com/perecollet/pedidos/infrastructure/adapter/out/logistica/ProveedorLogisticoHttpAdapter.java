package com.perecollet.pedidos.infrastructure.adapter.out.logistica;

import com.perecollet.pedidos.application.exception.ProveedorLogisticoException;
import com.perecollet.pedidos.application.exception.ProveedorNoDisponibleException;
import com.perecollet.pedidos.application.exception.ProveedorTimeoutException;
import com.perecollet.pedidos.application.port.out.ProveedorLogisticoPort;
import com.perecollet.pedidos.domain.model.Dinero;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.util.Currency;
import java.util.Set;

@Component
public class ProveedorLogisticoHttpAdapter implements ProveedorLogisticoPort {

    private static final Set<Integer> ESTADOS_TEMPORALES = Set.of(429, 502, 503, 504);
    private static final BigDecimal COSTE_MAXIMO = new BigDecimal("9999999999.99"); // NUMERIC(12, 2) en BD

    private final RestClient restClient;

    public ProveedorLogisticoHttpAdapter(RestClient proveedorLogisticoRestClient) {
        this.restClient = proveedorLogisticoRestClient;
    }

    @Override
    @Retryable(includes = ProveedorNoDisponibleException.class,
            maxRetriesString = "${proveedor-logistico.reintentos.max:2}",
            delayString = "${proveedor-logistico.reintentos.espera:500ms}",
            jitterString = "${proveedor-logistico.reintentos.jitter:100ms}",
            multiplier = 2)
    public Dinero obtenerTarifa(String codigoPostalDestino, BigDecimal pesoKg, int numBultos) {
        TarifaResponse respuesta;
        try {
            respuesta = restClient.post().uri("/api/tarifas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TarifaRequest(codigoPostalDestino, pesoKg, numBultos))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (peticion, res) -> {
                        int estado = res.getStatusCode().value();
                        if (ESTADOS_TEMPORALES.contains(estado)) {
                            throw new ProveedorNoDisponibleException("El proveedor respondió " + estado);
                        }
                        throw new ProveedorLogisticoException("El proveedor respondió " + estado);
                    })
                    .body(TarifaResponse.class);
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof HttpTimeoutException) {
                throw new ProveedorTimeoutException("El proveedor no respondió a tiempo", e);
            }
            throw new ProveedorNoDisponibleException("No se pudo conectar con el proveedor", e);
        } catch (RestClientException e) {
            throw traducirFalloDeLectura(e);
        }
        return aDinero(respuesta);
    }

    private static ProveedorLogisticoException traducirFalloDeLectura(RestClientException e) {
        Throwable causa = e.getMostSpecificCause();
        if (causa instanceof HttpTimeoutException) {
            return new ProveedorTimeoutException("El proveedor no respondió a tiempo", e);
        }
        if (causa instanceof IOException) {
            return new ProveedorNoDisponibleException("Se interrumpió la lectura de la respuesta del proveedor", e);
        }
        return new ProveedorLogisticoException("Respuesta ilegible del proveedor", e);
    }

    private static Dinero aDinero(TarifaResponse respuesta) {
        if (respuesta == null || respuesta.coste() == null || respuesta.moneda() == null) {
            throw new ProveedorLogisticoException("Respuesta del proveedor sin coste o sin moneda");
        }
        if (respuesta.coste().compareTo(COSTE_MAXIMO) > 0) {
            throw new ProveedorLogisticoException("Coste del proveedor fuera de rango: " + respuesta);
        }
        try {
            return Dinero.of(respuesta.coste(), Currency.getInstance(respuesta.moneda()));
        } catch (IllegalArgumentException | ArithmeticException e) {
            throw new ProveedorLogisticoException("Respuesta del proveedor no válida: " + respuesta, e);
        }
    }
}

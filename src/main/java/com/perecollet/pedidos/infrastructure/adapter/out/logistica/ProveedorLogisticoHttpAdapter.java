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

import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.util.Currency;

@Component
public class ProveedorLogisticoHttpAdapter implements ProveedorLogisticoPort {

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
                        if (estado == 503) {
                            throw new ProveedorNoDisponibleException("El proveedor respondió 503");
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
            throw new ProveedorLogisticoException("Respuesta ilegible del proveedor", e);
        }
        return aDinero(respuesta);
    }

    private static Dinero aDinero(TarifaResponse respuesta) {
        if (respuesta == null || respuesta.coste() == null || respuesta.moneda() == null) {
            throw new ProveedorLogisticoException("Respuesta del proveedor sin coste o sin moneda");
        }
        try {
            return Dinero.of(respuesta.coste(), Currency.getInstance(respuesta.moneda()));
        } catch (IllegalArgumentException e) {
            throw new ProveedorLogisticoException("Respuesta del proveedor no válida: " + respuesta, e);
        }
    }
}

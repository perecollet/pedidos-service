# pedidos-service

Servicio de pedidos que calcula el coste de envío llamando a un proveedor logístico externo.
Java 17 · Spring Boot 4 · PostgreSQL 17 + Flyway · arquitectura hexagonal · Testcontainers · WireMock.

## Cómo arrancar

Necesitas Docker. Crea primero el `.env`, porque compose exige el token en todos sus comandos:

```bash
cp .env.example .env
```

### Con Docker Compose

Levanta la aplicación (puerto 8080), PostgreSQL 17 y un WireMock (puerto 8089) que responde siempre
una tarifa de 7.95 EUR. Para usar un proveedor real, define `PROVEEDOR_LOGISTICO_URL` en el `.env`.

```bash
docker compose up --build
docker compose down -v   # para empezar de cero (borra también la base de datos)
```

### Con `TestPedidosServiceApplication`

Levanta la aplicación con PostgreSQL en Testcontainers (requiere Java 17). El proveedor se indica aparte:

```bash
docker compose up -d wiremock
./mvnw spring-boot:test-run -Dspring-boot.run.arguments=--proveedor-logistico.base-url=http://localhost:8089
```

### Probar la API

Con los datos de ejemplo de `V2__seed.sql`:

```bash
curl -i localhost:8080/api/pedidos -H 'Content-Type: application/json' -d '{
  "clienteId": "11111111-1111-1111-1111-111111111111",
  "codigoPostalDestino": "43840",
  "lineas": [{ "productoId": "22222222-2222-2222-2222-222222222221", "cantidad": 2 }]
}'

curl -i -X POST localhost:8080/api/pedidos/<id>/confirmacion
```

## Tests

```bash
./mvnw test   # requiere Docker (Testcontainers)
```

- **Unitarios** de dominio (`Dinero`, `Pedido`, `LineaPedido`…) y de los casos de uso con Mockito.
  El cálculo del total que pide el enunciado está en `PedidoTest`.
- **Integración** contra PostgreSQL real (Testcontainers) para la persistencia, incluida la reserva atómica
  con dos hilos simultáneos, y contra WireMock para el adaptador del proveedor (reintentos, timeouts, errores).
- **Extremo a extremo** (`PedidoApiTest`): peticiones HTTP reales. Incluye el timeout del proveedor
  (504 y pedido en `ERROR_ENVIO`) y dos confirmaciones simultáneas (una sola llamada al proveedor y un 409).

## Decisiones de diseño

### Modelo de datos
El proveedor necesita código postal, peso y número de bultos, que no aparecen en el modelo del enunciado:
- El **código postal** de destino es un dato del pedido (5 dígitos).
- Los precios y pesos salen de un **catálogo de productos** en el servidor (seed de Flyway). Cada línea
  copia el precio y el peso del producto al crear el pedido, así que un cambio posterior del catálogo no lo altera.
- Peso enviado = Σ peso × cantidad; bultos = Σ cantidades.
- El cliente no envía importes: el DTO no tiene esos campos y los que lleguen se ignoran.

### Importes
`Dinero(BigDecimal, Currency)` es un value object (patrón Money), con escala según la moneda y redondeo
`HALF_EVEN`. No permite sumar monedas distintas. Los pedidos son siempre en EUR: una tarifa en otra
moneda se trata como respuesta no válida del proveedor.

### Estados del pedido

```
CREADO ──► CONFIRMANDO ──► CONFIRMADO
               │
               ▼
          ERROR_ENVIO ──► CONFIRMANDO   (se puede volver a confirmar)
```

Las transiciones las valida el dominio. No hay `CANCELADO`, porque ningún endpoint lo usaría.

### Concurrencia
La confirmación empieza con un `UPDATE` condicional atómico
(`CREADO`/`ERROR_ENVIO` → `CONFIRMANDO`). Solo la petición que actualiza la fila llama al proveedor.
PostgreSQL bloquea la fila, y la segunda petición vuelve a evaluar el `WHERE` y no actualiza nada.
Funciona también con varias instancias de la aplicación, sin bloqueos en Java.

- Pedido ya `CONFIRMADO` → 200 con el pedido (idempotente, sin llamar al proveedor).
- Pedido en `CONFIRMANDO` → 409.
- **No hay ninguna transacción abierta durante la llamada HTTP**: reserva y guardado son transacciones cortas.

### Resiliencia ante el proveedor

Timeouts: conexión 2 s y lectura 6 s (el proveedor puede tardar hasta 5 s). Se reintenta hasta 2 veces
(3 intentos en total), con espera exponencial desde 500 ms y jitter (`@Retryable` de Spring Framework 7), ante:

- Respuestas 429, 502, 503 y 504.
- Timeouts y errores de red.

El resto de errores (otros 4xx, 500, respuestas no válidas) no se reintentan.

| Situación | Respuesta | Estado del pedido |
|---|---|---|
| Timeout esperando cabeceras | 504 | `ERROR_ENVIO` |
| Proveedor no disponible / atasco leyendo el cuerpo | 503 | `ERROR_ENVIO` |
| Respuesta no válida (4xx, JSON roto, moneda ≠ EUR, coste fuera de rango) | 502 | `ERROR_ENVIO` |

Cualquier fallo después de la reserva deja el pedido en `ERROR_ENVIO`, de modo que se puede volver a confirmar.
El timeout leyendo el cuerpo da 503 y no 504 porque el cliente HTTP lo notifica igual que un corte de conexión.
La compresión gzip está desactivada: con ella, ese mismo timeout llegaba como `NullPointerException` y se
saltaba la traducción de errores.

Con los valores por defecto, una confirmación tarda unos **20 s en el peor caso**
(3 lecturas de 6 s más las esperas) y ocupa un hilo durante ese tiempo. Es un límite aceptado.

El **token** se lee de la variable de entorno `PROVEEDOR_LOGISTICO_TOKEN`: no está en el código, se enmascara
en el `toString()` de la configuración y la aplicación no arranca sin él.

### Arquitectura
Hexagonal: `domain` (Java puro), `application` (casos de uso y puertos) e `infrastructure` (REST, JPA, HTTP).
Las entidades JPA están separadas del dominio. El lenguaje de negocio está en español, como el enunciado y la API,
y lo técnico en inglés.

## Validación de la petición

- `codigoPostalDestino`: 5 dígitos.
- `cantidad` debe ser un entero entre 1 y 1000. Un decimal como `2.5` se rechaza con 400.
- Un texto numérico como `"3"` se acepta y se convierte a número: es la conversión por defecto de Jackson.
- Si un producto aparece en varias líneas, se guardan tal cual (importes, peso y bultos son los mismos).
- Errores en formato `ProblemDetail`: 400 validación, 404 pedido inexistente, 409 confirmación en curso,
  422 cliente o producto inexistente, 502/503/504 fallos del proveedor (mensaje genérico, detalle en el log).

## Qué falta y cómo lo abordaría

- **Recuperación de pedidos en `CONFIRMANDO`:** si el proceso muere a mitad, o la base de datos cae al guardar,
  el pedido se queda en `CONFIRMANDO`. Guardaría la fecha de la reserva y un job devolvería a `ERROR_ENVIO`
  los que lleven más de N minutos.
- **Circuit breaker** (Resilience4j): hoy cada petición agota sus reintentos aunque el proveedor esté caído.
- **Respetar `Retry-After`** en los 429.
- **Confirmación asíncrona** (202 + procesado en segundo plano), para no ocupar un hilo hasta 20 s.
- **`GET /api/pedidos/{id}`:** el enunciado no lo pide, así que la creación no devuelve `Location`.
  Permitiría consultar el estado de un pedido después de un 504 o un 409.
- **`@Version` o condición de estado en el guardado**, necesario en cuanto exista la recuperación de `CONFIRMANDO`.
- Límite de líneas por pedido, logs con el id del pedido y fixtures de test compartidos.

## Uso de IA

He usado Claude como apoyo durante todo el desarrollo:

- **Diseño:** discutimos las alternativas (dónde van los puertos, `@Retryable` frente a Resilience4j,
  `UPDATE` condicional frente a bloqueo optimista o pesimista, ids `UUID` con tipos propios…). Las decisiones,
  y por qué, son las de este README.
- **TDD:** la IA escribía los tests en rojo y yo implementaba el código de dominio, aplicación y persistencia.
- **Revisión:** al final, Claude Code revisó el proyecto y aplicó los arreglos que acordé (pedidos bloqueados
  en `CONFIRMANDO`, validación de entrada, timeout leyendo el cuerpo, arranque con Docker).

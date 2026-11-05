CREATE TABLE cliente (
                         id     UUID         PRIMARY KEY,
                         nombre VARCHAR(255) NOT NULL,
                         email  VARCHAR(255) NOT NULL
);

CREATE TABLE producto (
                          id      UUID           PRIMARY KEY,
                          nombre  VARCHAR(255)   NOT NULL,
                          precio  NUMERIC(12, 2) NOT NULL CHECK (precio >= 0),
                          moneda  VARCHAR(3)     NOT NULL,
                          peso_kg NUMERIC(10, 3) NOT NULL CHECK (peso_kg >= 0)
);

CREATE TABLE pedido (
                        id                    UUID           PRIMARY KEY,
                        cliente_id            UUID           NOT NULL REFERENCES cliente (id),
                        codigo_postal_destino VARCHAR(10)    NOT NULL,
                        fecha                 TIMESTAMPTZ    NOT NULL,
                        estado                VARCHAR(20)    NOT NULL,
                        moneda                VARCHAR(3)     NOT NULL,
                        importe_productos     NUMERIC(12, 2) NOT NULL,
                        coste_envio           NUMERIC(12, 2),
                        importe_total         NUMERIC(12, 2),
                        CONSTRAINT ck_pedido_estado
                            CHECK (estado IN ('CREADO', 'CONFIRMANDO', 'CONFIRMADO', 'ERROR_ENVIO')),
                        CONSTRAINT ck_pedido_coste_segun_estado
                            CHECK ((estado = 'CONFIRMADO') = (coste_envio IS NOT NULL AND importe_total IS NOT NULL))
);

CREATE TABLE linea_pedido (
                              pedido_id       UUID           NOT NULL REFERENCES pedido (id),
                              posicion        INTEGER        NOT NULL,
                              producto_id     UUID           NOT NULL REFERENCES producto (id),
                              cantidad        INTEGER        NOT NULL CHECK (cantidad > 0),
                              precio_unitario NUMERIC(12, 2) NOT NULL,
                              moneda          VARCHAR(3)     NOT NULL,
                              peso_kg         NUMERIC(10, 3) NOT NULL,
                              PRIMARY KEY (pedido_id, posicion)
);

CREATE INDEX ix_pedido_cliente ON pedido (cliente_id);
-- Datos de ejemplo: clientes y catálogo de productos (no hay endpoints para crearlos).
-- UUID fijos para poder usarlos en peticiones de prueba y en los tests.

INSERT INTO cliente (id, nombre, email) VALUES
                                            ('11111111-1111-1111-1111-111111111111', 'Ana García',   'ana.garcia@example.com'),
                                            ('11111111-1111-1111-1111-111111111112', 'Jordi Puig',   'jordi.puig@example.com');

INSERT INTO producto (id, nombre, precio, moneda, peso_kg) VALUES
                                                               ('22222222-2222-2222-2222-222222222221', 'Teclado mecánico',   49.99, 'EUR', 1.250),
                                                               ('22222222-2222-2222-2222-222222222222', 'Ratón inalámbrico',  19.90, 'EUR', 0.200),
                                                               ('22222222-2222-2222-2222-222222222223', 'Monitor 27 pulgadas', 229.00, 'EUR', 6.500);
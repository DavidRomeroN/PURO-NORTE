-- Despacho ya vive en V1 (estado_despacho, despachado_en).
-- Indice para listar pendientes de cocina con menos carga.
CREATE INDEX idx_pedido_items_estado_despacho ON pedido_items (estado_despacho);

-- Permite descartar la cuenta de una mesa sin cobrarla.
--
-- Hasta ahora, una vez creado el pedido la mesa quedaba ocupada hasta que se cobraba:
-- si los clientes se iban sin consumir, o el mozo abría la mesa equivocada, esa mesa
-- ya no se podía volver a usar en todo el día. Quitar los ítems tampoco la liberaba.
--
-- La cuenta anulada no se borra. Anular es la manera más cómoda de hacer desaparecer
-- una mesa que sí consumió, así que queda registrado quién lo hizo, cuándo y por qué.
--
-- Ejecutar una sola vez sobre una base de datos que ya existe.
-- En instalaciones nuevas no hace falta: schema.sql ya viene con estos cambios.
-- Si sale "ERROR 1060 Duplicate column name", ya estaba aplicado y no hay nada que hacer.
--
-- PowerShell no acepta "<" para redirigir un archivo, hay que pasar por cmd:
--   cmd /c '"C:\laragon\bin\mysql\mysql-8.0.30-winx64\bin\mysql.exe" -u root puro_norte < "ruta\migracion-anular-pedido.sql"'

USE puro_norte;

ALTER TABLE pedidos
    MODIFY COLUMN estado ENUM('ABIERTO', 'CERRADO', 'PAGADO', 'ANULADO') NOT NULL DEFAULT 'ABIERTO',
    ADD COLUMN anulado_en       DATETIME     NULL AFTER cerrado_en,
    ADD COLUMN anulado_por      BIGINT       NULL AFTER anulado_en,
    ADD COLUMN motivo_anulacion VARCHAR(300) NULL AFTER anulado_por,
    ADD CONSTRAINT fk_pedidos_anulado_por FOREIGN KEY (anulado_por) REFERENCES usuarios(id);

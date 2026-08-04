-- Integracion real con FactuSmart: la boleta necesita guardar el identificador
-- que devuelve el proveedor y el resultado que dio SUNAT.
--
-- Ejecutar una sola vez sobre una base de datos que ya existe.
-- En instalaciones nuevas no hace falta: schema.sql ya viene con estos cambios.
-- Si sale "ERROR 1060 Duplicate column name", ya estaba aplicado y no hay nada que hacer.
--
-- PowerShell no acepta "<" para redirigir un archivo, hay que pasar por cmd:
--   cmd /c '"C:\laragon\bin\mysql\mysql-8.0.30-winx64\bin\mysql.exe" -u root puro_norte < "ruta\migracion-factusmart.sql"'
--
-- external_id es el dato mas importante: es el unico identificador con el que se
-- puede consultar estado, reenviar o descargar el PDF despues. Sin el, el
-- comprobante queda huerfano en el sistema del proveedor.

USE puro_norte;

ALTER TABLE boletas
    ADD COLUMN external_id        VARCHAR(64)  NULL AFTER pedido_id,
    ADD COLUMN sunat_codigo       VARCHAR(10)  NULL,
    ADD COLUMN sunat_descripcion  VARCHAR(500) NULL,
    ADD COLUMN cliente_documento  VARCHAR(15)  NULL,
    ADD COLUMN intentos_envio     INT NOT NULL DEFAULT 0,
    ADD COLUMN ultimo_intento_en  DATETIME NULL,
    ADD INDEX idx_boletas_external_id (external_id),
    ADD INDEX idx_boletas_estado (estado_sunat);

-- La API no entrega URLs permanentes sino endpoints que se consultan con el
-- external_id, asi que estas tres columnas quedan sin uso. Se dejan por si hay
-- datos viejos; los enlaces se arman bajo demanda desde el backend.

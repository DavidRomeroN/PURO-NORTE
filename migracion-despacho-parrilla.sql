-- Estado de despacho por plato, para la pantalla del parrillero.
--
-- Cada ítem nace pendiente. El parrillero marca el que ya salió; los que faltan
-- siguen en la lista. Un pedido con todos los platos despachados deja de aparecer
-- como pendiente en la parrilla, pero la cuenta sigue abierta hasta cobrarla.
--
-- Ejecutar una sola vez sobre una base que ya existe.
-- En instalaciones nuevas no hace falta: schema.sql ya viene con estos cambios.
-- Si sale "ERROR 1060 Duplicate column name", ya estaba aplicado.
--
-- PowerShell no acepta "<" para redirigir: hay que pasar por cmd:
--   cmd /c '"C:\laragon\bin\mysql\mysql-8.0.30-winx64\bin\mysql.exe" -u root puro_norte < "ruta\migracion-despacho-parrilla.sql"'

USE puro_norte;

ALTER TABLE pedido_items
    ADD COLUMN estado_despacho ENUM('PENDIENTE', 'DESPACHADO') NOT NULL DEFAULT 'PENDIENTE'
        AFTER para_llevar,
    ADD COLUMN despachado_en DATETIME NULL AFTER estado_despacho;

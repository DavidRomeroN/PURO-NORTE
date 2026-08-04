-- Pedidos para llevar: el cliente no ocupa mesa.
-- Ejecutar una sola vez sobre una base de datos que ya existe.
-- En instalaciones nuevas no hace falta: schema.sql ya viene con estos cambios.
-- Si al correrlo sale "ERROR 1060 Duplicate column name", ya estaba aplicado y no hay nada que hacer.
--
-- PowerShell no acepta "<" para redirigir un archivo, hay que pasar por cmd:
--   cmd /c '"C:\laragon\bin\mysql\mysql-8.0.30-winx64\bin\mysql.exe" -u root puro_norte < "ruta\migracion-para-llevar.sql"'

USE puro_norte;

ALTER TABLE pedidos
    MODIFY COLUMN mesa_id BIGINT NULL,
    ADD COLUMN numero_llevar INT NULL AFTER mesa_id;

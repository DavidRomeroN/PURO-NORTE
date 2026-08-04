-- Marca las boletas emitidas en modo simulado.
--
-- Sin API key de FactuSmart la app finge la emision y la boleta queda como ACEPTADO,
-- igual que una real. Al mirar la base despues no habia forma de distinguirlas, asi
-- que una venta de prueba parecia una venta con comprobante valido ante SUNAT.
--
-- Ejecutar una sola vez sobre una base de datos que ya existe.
-- En instalaciones nuevas no hace falta: schema.sql ya viene con este cambio.
-- Si sale "ERROR 1060 Duplicate column name", ya estaba aplicado y no hay nada que hacer.
--
-- PowerShell no acepta "<" para redirigir un archivo, hay que pasar por cmd:
--   cmd /c '"C:\laragon\bin\mysql\mysql-8.0.30-winx64\bin\mysql.exe" -u root puro_norte < "ruta\migracion-boleta-simulada.sql"'

USE puro_norte;

ALTER TABLE boletas
    ADD COLUMN simulada BOOLEAN NOT NULL DEFAULT FALSE AFTER estado_sunat;

-- Las boletas que ya estaban en la base nunca salieron a la red, y se marcan para no
-- confundirlas con las reales. Son dos casos: las del modo simulado actual, que llevan
-- external_id 'SIMULADO-n', y las del stub anterior, que se guardaron como aceptadas sin
-- external_id. Una aceptacion de verdad siempre trae external_id, asi que no se toca
-- ninguna boleta legitima. Las pendientes o con error se dejan como estan: esas si
-- pueden reintentarse.
UPDATE boletas
SET simulada = TRUE
WHERE external_id LIKE 'SIMULADO-%'
   OR (external_id IS NULL AND estado_sunat = 'ACEPTADO');

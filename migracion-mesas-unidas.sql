-- Unir mesas: un grupo grande junta la 12 con la 13, se sienta en las dos y paga
-- una sola cuenta. La mesa principal sigue siendo pedidos.mesa_id; aca van las demas.
--
-- Ejecutar una sola vez sobre una base de datos que ya existe.
-- En instalaciones nuevas no hace falta: schema.sql ya viene con estos cambios.
-- Si sale "ERROR 1050 Table already exists", ya estaba aplicado y no hay nada que hacer.
--
-- PowerShell no acepta "<" para redirigir un archivo, hay que pasar por cmd:
--   cmd /c '"C:\laragon\bin\mysql\mysql-8.0.30-winx64\bin\mysql.exe" -u root puro_norte < "ruta\migracion-mesas-unidas.sql"'
--
-- No hay UNIQUE sobre mesa_id a proposito: las filas se conservan como historia de
-- que mesas ocupo cada cuenta, y una mesa se une muchas veces a lo largo del tiempo.
-- Que una mesa no este en dos cuentas vivas a la vez lo valida el backend.

USE puro_norte;

CREATE TABLE pedido_mesas_unidas (
    pedido_id   BIGINT NOT NULL,
    mesa_id     BIGINT NOT NULL,
    PRIMARY KEY (pedido_id, mesa_id),
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    FOREIGN KEY (mesa_id) REFERENCES mesas(id),
    INDEX idx_pedido_mesas_unidas_mesa (mesa_id)
);

-- =========================================================
-- Esquema de base de datos - Sistema Anticuchería
-- MySQL 8.x
-- =========================================================

-- Sin esto las tablas heredan el charset del servidor, que en Windows suele ser
-- latin1, y los nombres con tilde se guardan como "CorazÃ³n".
CREATE DATABASE IF NOT EXISTS puro_norte
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE puro_norte;

-- ---------------------------------------------------------
-- USUARIOS Y ROLES
-- ---------------------------------------------------------
CREATE TABLE usuarios (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    usuario         VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    rol             ENUM('MOZO', 'CAJA', 'ADMIN') NOT NULL,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------
-- MESAS
-- ---------------------------------------------------------
CREATE TABLE mesas (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero          INT NOT NULL UNIQUE,
    estado          ENUM('LIBRE', 'OCUPADA') NOT NULL DEFAULT 'LIBRE'
);

-- ---------------------------------------------------------
-- CATÁLOGO BASE
-- Componentes de anticuchos (corazón, carne, pollo, etc.),
-- bebidas y extras (taper, bandeja, papa)
-- ---------------------------------------------------------
CREATE TABLE productos_base (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre           VARCHAR(50) NOT NULL,
    tipo             ENUM('ANTICUCHO', 'BEBIDA', 'EXTRA') NOT NULL,
    precio_unitario  DECIMAL(6,2) NOT NULL,
    activo           BOOLEAN NOT NULL DEFAULT TRUE
);

-- Seed sugerido:
-- ANTICUCHO: Corazón 6.00 | Carne 7.00 | Salchicha 4.00 (precio de par) | Pollo 6.00 | Molleja 6.00 | Chorizo 6.00
-- BEBIDA:    Gaseosa x.xx | Mate x.xx | Emoliente x.xx
-- EXTRA:     Taper 1.00 | Bandeja 0.50 | Papa x.xx

-- ---------------------------------------------------------
-- COMBOS (Mixto Simple / Mixto Especial)
-- ---------------------------------------------------------
CREATE TABLE combos (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(50) NOT NULL,
    precio_base     DECIMAL(6,2) NOT NULL,
    activo          BOOLEAN NOT NULL DEFAULT TRUE
);
-- Seed: Mixto Simple 17.00 | Mixto Especial 23.00

-- Composición estándar de cada combo: qué trae por defecto,
-- si es sustituible y si es cortesía (nunca sustituible ni afecta precio)
CREATE TABLE combo_slots (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    combo_id                    BIGINT NOT NULL,
    producto_base_default_id    BIGINT NOT NULL,
    es_cortesia                 BOOLEAN NOT NULL DEFAULT FALSE,
    es_sustituible               BOOLEAN NOT NULL DEFAULT TRUE,
    orden                       INT NOT NULL,
    FOREIGN KEY (combo_id) REFERENCES combos(id),
    FOREIGN KEY (producto_base_default_id) REFERENCES productos_base(id)
);
-- Seed Mixto Simple:   slot1 Pollo (sustituible) | slot2 Carne (sustituible)
--                      | slot3 Corazón (sustituible) | slot4 Salchicha (cortesía, NO sustituible)
-- Seed Mixto Especial: los mismos 4 + slot5 Chorizo (sustituible)

-- ---------------------------------------------------------
-- PEDIDOS
-- Una mesa puede acumular varios pedidos hasta que se cierra la cuenta.
-- mesa_id nulo = pedido para llevar; en ese caso numero_llevar lo identifica
-- ("Para llevar 1", "Para llevar 2"...) y se reinicia cada día.
-- ---------------------------------------------------------
CREATE TABLE pedidos (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    mesa_id         BIGINT NULL,
    numero_llevar   INT NULL,
    mozo_id         BIGINT NOT NULL,
    estado          ENUM('ABIERTO', 'CERRADO', 'PAGADO', 'ANULADO') NOT NULL DEFAULT 'ABIERTO',
    creado_en       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cerrado_en      DATETIME NULL,
    -- Anular descarta la cuenta y libera las mesas sin cobrar. Queda con nombre y
    -- motivo porque es la forma más fácil de hacer desaparecer una mesa que sí consumió.
    anulado_en      DATETIME NULL,
    anulado_por     BIGINT NULL,
    motivo_anulacion VARCHAR(300) NULL,
    FOREIGN KEY (mesa_id) REFERENCES mesas(id),
    FOREIGN KEY (mozo_id) REFERENCES usuarios(id),
    FOREIGN KEY (anulado_por) REFERENCES usuarios(id)
);

-- ---------------------------------------------------------
-- MESAS UNIDAS A UN PEDIDO
-- Un grupo grande junta la 12 con la 13, se sienta en las dos y paga una sola
-- cuenta. La mesa principal es pedidos.mesa_id; aquí van las demás.
-- Sin UNIQUE sobre mesa_id a propósito: las filas quedan como historia y una
-- mesa se une muchas veces con el tiempo. Que no esté en dos cuentas vivas a la
-- vez lo valida el backend.
-- ---------------------------------------------------------
CREATE TABLE pedido_mesas_unidas (
    pedido_id   BIGINT NOT NULL,
    mesa_id     BIGINT NOT NULL,
    PRIMARY KEY (pedido_id, mesa_id),
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    FOREIGN KEY (mesa_id) REFERENCES mesas(id),
    INDEX idx_pedido_mesas_unidas_mesa (mesa_id)
);

-- ---------------------------------------------------------
-- ÍTEMS DEL PEDIDO
-- precio_calculado guarda el snapshot ya calculado por el backend
-- (suma de componentes o precio_base + sustituciones)
-- ---------------------------------------------------------
CREATE TABLE pedido_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id           BIGINT NOT NULL,
    tipo_item           ENUM('ANTICUCHO', 'COMBO', 'BEBIDA', 'EXTRA') NOT NULL,
    combo_id            BIGINT NULL,               -- solo si tipo_item = COMBO
    cantidad            INT NOT NULL DEFAULT 1,
    precio_calculado    DECIMAL(6,2) NOT NULL,      -- lo que el motor de precios calculó solo
    precio_final        DECIMAL(6,2) NOT NULL,      -- lo que realmente se cobra (= precio_calculado si no se editó)
    editado_manualmente BOOLEAN NOT NULL DEFAULT FALSE,
    editado_por         BIGINT NULL,                -- usuario (caja/admin) que hizo el cambio
    motivo_edicion      VARCHAR(255) NULL,
    para_llevar         BOOLEAN NOT NULL DEFAULT FALSE,
    -- El parrillero marca cada plato cuando sale. Lo pendiente sigue en su lista.
    estado_despacho     ENUM('PENDIENTE', 'DESPACHADO') NOT NULL DEFAULT 'PENDIENTE',
    despachado_en       DATETIME NULL,
    observaciones       VARCHAR(255) NULL,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    FOREIGN KEY (combo_id) REFERENCES combos(id),
    FOREIGN KEY (editado_por) REFERENCES usuarios(id)
);

-- ---------------------------------------------------------
-- COMPONENTES DE CADA ÍTEM
-- Anticucho simple/doble/triple: 1 fila por componente elegido
-- Combo: 1 fila por slot, marcando si fue sustituido del default
-- precio_unitario_snapshot: precio vigente al momento de la venta
-- (para que boletas antiguas no cambien si el precio sube después)
-- ---------------------------------------------------------
CREATE TABLE pedido_item_componentes (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_item_id              BIGINT NOT NULL,
    producto_base_id            BIGINT NOT NULL,
    combo_slot_id               BIGINT NULL,        -- solo si el ítem es COMBO
    es_sustitucion               BOOLEAN NOT NULL DEFAULT FALSE,
    precio_unitario_snapshot    DECIMAL(6,2) NOT NULL,
    FOREIGN KEY (pedido_item_id) REFERENCES pedido_items(id),
    FOREIGN KEY (producto_base_id) REFERENCES productos_base(id),
    FOREIGN KEY (combo_slot_id) REFERENCES combo_slots(id)
);

-- ---------------------------------------------------------
-- BOLETAS
-- ---------------------------------------------------------
CREATE TABLE boletas (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id           BIGINT NOT NULL,
    -- Identificador que devuelve FactuSmart. Es el unico con el que se puede
    -- consultar estado, reenviar o descargar el PDF despues.
    external_id         VARCHAR(64) NULL,
    tipo                ENUM('CONSUMO', 'DETALLADO') NOT NULL,
    serie               VARCHAR(10) NULL,
    correlativo         VARCHAR(15) NULL,
    monto_total         DECIMAL(8,2) NOT NULL,
    forma_pago          ENUM('CONTADO', 'CREDITO') NOT NULL DEFAULT 'CONTADO',
    medio_pago          ENUM('EFECTIVO', 'TARJETA', 'YAPE', 'PLIN', 'OTRO') NOT NULL,
    estado_sunat        ENUM('PENDIENTE', 'ACEPTADO', 'OBSERVADO', 'ERROR') NOT NULL DEFAULT 'PENDIENTE',
    -- Emitida en modo simulado: figura ACEPTADO pero no existe ante SUNAT ni tiene
    -- validez fiscal. Solo ocurre en desarrollo, sin API key de FactuSmart.
    simulada            BOOLEAN NOT NULL DEFAULT FALSE,
    sunat_codigo        VARCHAR(10) NULL,
    sunat_descripcion   VARCHAR(500) NULL,
    -- DNI cuando el cliente lo da. Obligatorio desde S/700 por regla de SUNAT.
    cliente_documento   VARCHAR(15) NULL,
    intentos_envio      INT NOT NULL DEFAULT 0,
    ultimo_intento_en   DATETIME NULL,
    xml_url             VARCHAR(255) NULL,
    pdf_url             VARCHAR(255) NULL,
    cdr_url             VARCHAR(255) NULL,
    emitido_en          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cajero_id           BIGINT NOT NULL,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    FOREIGN KEY (cajero_id) REFERENCES usuarios(id),
    INDEX idx_boletas_external_id (external_id),
    INDEX idx_boletas_estado (estado_sunat)
);

-- Detalle de boleta: solo se llena si boletas.tipo = 'DETALLADO'
-- Si tipo = 'CONSUMO' esta tabla queda vacía y monto_total basta.
CREATE TABLE boleta_detalle (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    boleta_id           BIGINT NOT NULL,
    descripcion         VARCHAR(255) NOT NULL,
    cantidad            INT NOT NULL DEFAULT 1,
    precio_unitario     DECIMAL(6,2) NOT NULL,
    subtotal            DECIMAL(8,2) NOT NULL,
    FOREIGN KEY (boleta_id) REFERENCES boletas(id)
);

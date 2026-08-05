ALTER TABLE boletas
    ADD COLUMN token_publico VARCHAR(64) NULL,
    ADD COLUMN enviada_whatsapp TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN enviada_correo TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN enviada_en DATETIME NULL;

CREATE UNIQUE INDEX uk_boletas_token_publico ON boletas (token_publico);

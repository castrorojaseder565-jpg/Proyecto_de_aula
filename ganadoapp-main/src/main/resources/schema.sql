CREATE TABLE IF NOT EXISTS invitacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(254) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    creada_en DATETIME NOT NULL,
    expira_en DATETIME NOT NULL,
    usada_en DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invitacion_token_hash (token_hash),
    KEY ix_invitacion_correo (correo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

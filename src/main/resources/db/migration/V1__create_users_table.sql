-- =============================================================
-- INFINITY GRAFF — Migration V1
-- Criação da tabela de usuários com soft delete
-- =============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE usuarios (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    nome                VARCHAR(100)    NOT NULL,
    email               VARCHAR(150)    NOT NULL,
    senha_hash          VARCHAR(255)    NOT NULL,
    role                VARCHAR(20)     NOT NULL,
    ativo               BOOLEAN         NOT NULL DEFAULT TRUE,
    aceito_termos_em    TIMESTAMPTZ     NOT NULL,
    criado_em           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deletado_em         TIMESTAMPTZ     NULL,

    CONSTRAINT pk_usuarios PRIMARY KEY (id),

    CONSTRAINT ck_usuarios_role CHECK (
        role IN ('ADMIN', 'GERENTE', 'PRESTADOR', 'CLIENTE')
    )
);

-- E-mail único globalmente, inclusive após soft delete.
-- A aplicação também deve normalizar email com trim() + lowercase().
CREATE UNIQUE INDEX idx_usuarios_email_lower
    ON usuarios (LOWER(email));

-- Índice para filtro por role em usuários não deletados
CREATE INDEX idx_usuarios_role
    ON usuarios (role)
    WHERE deletado_em IS NULL;

-- Índice para filtro por status ativo em usuários não deletados
CREATE INDEX idx_usuarios_ativo
    ON usuarios (ativo)
    WHERE deletado_em IS NULL;

-- Índice parcial para consultas que filtram registros não deletados
CREATE INDEX idx_usuarios_deletado_em
    ON usuarios (deletado_em)
    WHERE deletado_em IS NULL;

COMMENT ON TABLE  usuarios              IS 'Usuários da plataforma INFINITY GRAFF';
COMMENT ON COLUMN usuarios.id           IS 'Identificador único UUID gerado pelo banco';
COMMENT ON COLUMN usuarios.nome         IS 'Nome completo do usuário';
COMMENT ON COLUMN usuarios.email        IS 'E-mail de acesso — único globalmente, inclusive após soft delete';
COMMENT ON COLUMN usuarios.senha_hash   IS 'Hash BCrypt custo 12 da senha do usuário';
COMMENT ON COLUMN usuarios.role         IS 'Perfil de acesso: ADMIN, GERENTE, PRESTADOR ou CLIENTE';
COMMENT ON COLUMN usuarios.ativo        IS 'Indica se o usuário pode fazer login';
COMMENT ON COLUMN usuarios.aceito_termos_em IS 'Momento em que o usuário aceitou os termos de uso';
COMMENT ON COLUMN usuarios.criado_em    IS 'Timestamp de criação do registro em UTC';
COMMENT ON COLUMN usuarios.atualizado_em IS 'Timestamp da última atualização em UTC';
COMMENT ON COLUMN usuarios.deletado_em  IS 'Timestamp do soft delete — NULL significa não deletado';
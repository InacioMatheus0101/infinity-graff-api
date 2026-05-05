-- =============================================================
-- INFINITY GRAFF — Migration V1
-- Criação da tabela de usuários internos com Supabase Auth
-- =============================================================
-- Decisão arquitetural:
--   - O login/cadastro/sessão são controlados pelo Supabase Auth.
--   - O backend Spring não armazena senha.
--   - usuarios.id é o mesmo UUID do usuário no Supabase Auth.
--   - O backend usa esta tabela para controlar perfil, role, status e regras internas.
-- =============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE usuarios (
    id                  UUID            NOT NULL,
    nome                VARCHAR(100)    NOT NULL,
    email               VARCHAR(150)    NOT NULL,
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

-- Índice para filtro por role em usuários não deletados.
CREATE INDEX idx_usuarios_role
    ON usuarios (role)
    WHERE deletado_em IS NULL;

-- Índice para filtro por status ativo em usuários não deletados.
CREATE INDEX idx_usuarios_ativo
    ON usuarios (ativo)
    WHERE deletado_em IS NULL;

-- Índice parcial para consultas que filtram registros não deletados.
CREATE INDEX idx_usuarios_deletado_em
    ON usuarios (deletado_em)
    WHERE deletado_em IS NULL;

COMMENT ON TABLE usuarios IS 'Perfis internos dos usuários autenticados pelo Supabase Auth';
COMMENT ON COLUMN usuarios.id IS 'UUID do usuário no Supabase Auth';
COMMENT ON COLUMN usuarios.nome IS 'Nome completo do usuário no marketplace';
COMMENT ON COLUMN usuarios.email IS 'E-mail do usuário — único globalmente, inclusive após soft delete';
COMMENT ON COLUMN usuarios.role IS 'Perfil interno de acesso: ADMIN, GERENTE, PRESTADOR ou CLIENTE';
COMMENT ON COLUMN usuarios.ativo IS 'Indica se o usuário pode usar o sistema interno';
COMMENT ON COLUMN usuarios.aceito_termos_em IS 'Momento em que o usuário aceitou os termos de uso';
COMMENT ON COLUMN usuarios.criado_em IS 'Timestamp de criação do registro em UTC';
COMMENT ON COLUMN usuarios.atualizado_em IS 'Timestamp da última atualização em UTC';
COMMENT ON COLUMN usuarios.deletado_em IS 'Timestamp do soft delete — NULL significa não deletado';
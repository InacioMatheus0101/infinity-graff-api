-- =============================================================
-- INFINITY GRAFF — Migration V2
-- Criação da tabela de refresh tokens
-- =============================================================
-- Segurança:
--   - Apenas o hash SHA-256 do token é armazenado
--   - O token puro nunca é persistido
--   - Rotação obrigatória: cada uso invalida o token atual
--     e gera um novo par (access + refresh)
-- =============================================================

CREATE TABLE refresh_tokens (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    usuario_id          UUID            NOT NULL,
    token_hash          VARCHAR(64)     NOT NULL,
    expira_em           TIMESTAMPTZ     NOT NULL,
    revogado            BOOLEAN         NOT NULL DEFAULT FALSE,
    revogado_em         TIMESTAMPTZ     NULL,
    criado_em           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    ip_criacao          VARCHAR(100)    NULL,
    user_agent_criacao  TEXT            NULL,

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id)
        ON DELETE CASCADE
);

-- Índice para buscar tokens de um usuário
CREATE INDEX idx_refresh_tokens_usuario_id
    ON refresh_tokens (usuario_id);

-- Índice composto para buscar tokens válidos de um usuário
CREATE INDEX idx_refresh_tokens_busca
    ON refresh_tokens (usuario_id, revogado, expira_em);

-- Índice para limpeza futura de tokens expirados não revogados
CREATE INDEX idx_refresh_tokens_expira_em
    ON refresh_tokens (expira_em)
    WHERE revogado = FALSE;

COMMENT ON TABLE  refresh_tokens                    IS 'Refresh tokens para renovação de sessão JWT';
COMMENT ON COLUMN refresh_tokens.id                 IS 'Identificador único UUID';
COMMENT ON COLUMN refresh_tokens.usuario_id         IS 'Referência ao usuário dono do token';
COMMENT ON COLUMN refresh_tokens.token_hash         IS 'Hash SHA-256 do token — o token puro nunca é armazenado';
COMMENT ON COLUMN refresh_tokens.expira_em          IS 'Momento de expiração do token em UTC';
COMMENT ON COLUMN refresh_tokens.revogado           IS 'TRUE quando o token foi usado na rotação, invalidado por logout ou revogado administrativamente';
COMMENT ON COLUMN refresh_tokens.revogado_em        IS 'Momento em que o token foi revogado em UTC';
COMMENT ON COLUMN refresh_tokens.criado_em          IS 'Timestamp de criação em UTC';
COMMENT ON COLUMN refresh_tokens.ip_criacao         IS 'IP do request que originou o token';
COMMENT ON COLUMN refresh_tokens.user_agent_criacao IS 'User-Agent do request que originou o token';
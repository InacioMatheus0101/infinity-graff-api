-- =============================================================
-- INFINITY GRAFF — Migration V3
-- Criação da tabela de logs de auditoria
-- =============================================================
-- Design:
--   - Registros são imutáveis pela regra da aplicação
--   - usuario_id é nullable para ações anônimas, como LOGIN_FALHA
--   - dados_antes e dados_depois são armazenados como TEXT contendo JSON serializado
--   - ip e user_agent são registrados para rastreabilidade
-- =============================================================

CREATE TABLE logs_auditoria (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    usuario_id      UUID            NULL,
    acao            VARCHAR(100)    NOT NULL,
    entidade        VARCHAR(50)     NULL,
    entidade_id     UUID            NULL,
    dados_antes     TEXT            NULL,
    dados_depois    TEXT            NULL,
    ip              VARCHAR(100)    NULL,
    user_agent      TEXT            NULL,
    criado_em       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_logs_auditoria PRIMARY KEY (id),
    CONSTRAINT fk_logs_auditoria_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id)
        ON DELETE SET NULL
);

-- Índice para filtro por usuário
CREATE INDEX idx_logs_usuario_id
    ON logs_auditoria (usuario_id);

-- Índice para filtro por ação
CREATE INDEX idx_logs_acao
    ON logs_auditoria (acao);

-- Índice para filtro por entidade
CREATE INDEX idx_logs_entidade
    ON logs_auditoria (entidade);

-- Índice para ordenação/filtro por período
CREATE INDEX idx_logs_criado_em
    ON logs_auditoria (criado_em DESC);

-- Índice composto opcional para auditoria por usuário e período
CREATE INDEX idx_logs_usuario_criado_em
    ON logs_auditoria (usuario_id, criado_em DESC)
    WHERE usuario_id IS NOT NULL;

COMMENT ON TABLE  logs_auditoria             IS 'Log imutável de ações sensíveis na plataforma';
COMMENT ON COLUMN logs_auditoria.id          IS 'Identificador único UUID';
COMMENT ON COLUMN logs_auditoria.usuario_id  IS 'Usuário que realizou a ação — nullable para ações anônimas';
COMMENT ON COLUMN logs_auditoria.acao        IS 'Tipo de evento auditado';
COMMENT ON COLUMN logs_auditoria.entidade    IS 'Nome da entidade afetada, exemplo: usuarios, refresh_tokens';
COMMENT ON COLUMN logs_auditoria.entidade_id IS 'ID da entidade afetada quando aplicável';
COMMENT ON COLUMN logs_auditoria.dados_antes IS 'Estado anterior da entidade em JSON serializado, quando aplicável';
COMMENT ON COLUMN logs_auditoria.dados_depois IS 'Estado posterior da entidade em JSON serializado, quando aplicável';
COMMENT ON COLUMN logs_auditoria.ip          IS 'IP do cliente ou origem identificada pelo backend';
COMMENT ON COLUMN logs_auditoria.user_agent  IS 'User-Agent do cliente HTTP';
COMMENT ON COLUMN logs_auditoria.criado_em   IS 'Timestamp imutável de criação do log em UTC';
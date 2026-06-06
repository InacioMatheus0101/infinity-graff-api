-- ============================================================
-- V7 — Tabela de mensagens do chat (Cliente ↔ Prestador)
-- ============================================================

CREATE TABLE IF NOT EXISTS mensagens_chat (
    id                UUID PRIMARY KEY,
    solicitacao_id    UUID NOT NULL,
    remetente_id      UUID NOT NULL,
    destinatario_id   UUID NOT NULL,
    conteudo          TEXT NOT NULL,
    editada           BOOLEAN NOT NULL DEFAULT FALSE,
    lida              BOOLEAN NOT NULL DEFAULT FALSE,
    lida_em           TIMESTAMPTZ,
    deletada          BOOLEAN NOT NULL DEFAULT FALSE,
    deletada_em       TIMESTAMPTZ,
    criado_em         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expira_em         TIMESTAMPTZ NOT NULL,

    -- Foreign Keys com proteção contra remoção acidental
    CONSTRAINT fk_chat_solicitacao FOREIGN KEY (solicitacao_id)
        REFERENCES solicitacoes_arte(id) ON DELETE RESTRICT,
    CONSTRAINT fk_chat_remetente FOREIGN KEY (remetente_id)
        REFERENCES usuarios(id) ON DELETE RESTRICT,
    CONSTRAINT fk_chat_destinatario FOREIGN KEY (destinatario_id)
        REFERENCES usuarios(id) ON DELETE RESTRICT,

    -- Consistência do soft delete (deletada e deletada_em devem ser coerentes)
    CONSTRAINT chk_chat_deletada_em CHECK (
        (deletada = FALSE AND deletada_em IS NULL) OR
        (deletada = TRUE  AND deletada_em IS NOT NULL)
    ),

    -- Conteúdo não pode ser vazio (após remover espaços)
    CONSTRAINT chk_chat_conteudo_nao_vazio CHECK (LENGTH(TRIM(conteudo)) > 0)
);

-- Índice para listagem cronológica das mensagens de uma solicitação
CREATE INDEX idx_mensagens_solicitacao
    ON mensagens_chat (solicitacao_id, criado_em ASC);

-- Índice para job de limpeza de mensagens expiradas
CREATE INDEX idx_mensagens_expiradas
    ON mensagens_chat (expira_em)
    WHERE deletada = FALSE;
-- ============================================================
-- V6 — Tabela de arquivos anexados às solicitações de arte
-- ============================================================

CREATE TABLE IF NOT EXISTS arquivos_solicitacao_arte (
    id                    UUID PRIMARY KEY,
    solicitacao_id        UUID NOT NULL,
    enviado_por_id        UUID NOT NULL,
    tipo                  VARCHAR(30) NOT NULL,
    nome_original         VARCHAR(255) NOT NULL,
    nome_armazenado       VARCHAR(255) NOT NULL,
    content_type          VARCHAR(100) NOT NULL,
    tamanho_bytes         BIGINT NOT NULL,
    storage_bucket        VARCHAR(100) NOT NULL,
    storage_path_original TEXT NOT NULL,
    storage_path_medio    TEXT,
    storage_path_baixo    TEXT,
    descricao             VARCHAR(500),
    removido_em           TIMESTAMPTZ,
    removido_por_id       UUID,
    criado_em             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deletado_em           TIMESTAMPTZ,

    -- Foreign Keys
    CONSTRAINT fk_arquivos_solicitacao
        FOREIGN KEY (solicitacao_id)
        REFERENCES solicitacoes_arte(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_arquivos_enviado_por
        FOREIGN KEY (enviado_por_id)
        REFERENCES usuarios(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_arquivos_removido_por
        FOREIGN KEY (removido_por_id)
        REFERENCES usuarios(id)
        ON DELETE SET NULL,

    -- Constraints de domínio
    CONSTRAINT chk_arquivos_tipo
        CHECK (tipo IN ('REFERENCIA', 'ELEMENTO', 'RASCUNHO', 'PRINT', 'ARQUIVO_FINAL')),

    CONSTRAINT chk_arquivos_tamanho
        CHECK (tamanho_bytes > 0 AND tamanho_bytes <= 20971520),

    CONSTRAINT chk_arquivos_remocao_consistente
        CHECK (
            (removido_em IS NULL AND removido_por_id IS NULL)
            OR
            (removido_em IS NOT NULL AND removido_por_id IS NOT NULL)
        ),

    -- Unique constraint
    CONSTRAINT uq_arquivos_storage_path_original UNIQUE (storage_path_original)
);

-- Índices para consultas frequentes

-- Busca por solicitação (ativos)
CREATE INDEX idx_arquivos_solicitacao
    ON arquivos_solicitacao_arte(solicitacao_id)
    WHERE deletado_em IS NULL;

-- Busca por usuário que enviou
CREATE INDEX idx_arquivos_enviado_por
    ON arquivos_solicitacao_arte(enviado_por_id)
    WHERE deletado_em IS NULL;

-- Filtro por tipo
CREATE INDEX idx_arquivos_tipo
    ON arquivos_solicitacao_arte(tipo)
    WHERE deletado_em IS NULL;

-- Arquivos removidos (para job de limpeza futura)
CREATE INDEX idx_arquivos_removidos
    ON arquivos_solicitacao_arte(removido_em)
    WHERE removido_em IS NOT NULL;

-- Ordenação por data de criação (listagens)
CREATE INDEX idx_arquivos_criado_em
    ON arquivos_solicitacao_arte(criado_em DESC)
    WHERE deletado_em IS NULL;

-- Índice composto para consultas por solicitação + tipo
CREATE INDEX idx_arquivos_solicitacao_tipo
    ON arquivos_solicitacao_arte(solicitacao_id, tipo)
    WHERE deletado_em IS NULL;

-- Índice único para garantir unicidade do path original
CREATE UNIQUE INDEX idx_arquivos_path_original_unique
    ON arquivos_solicitacao_arte(storage_path_original);
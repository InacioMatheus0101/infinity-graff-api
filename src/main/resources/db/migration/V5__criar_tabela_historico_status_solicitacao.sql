-- =============================================================
-- INFINITY GRAFF — Migration V5
-- Criação da tabela de histórico de status das solicitações
-- =============================================================
-- Design:
--   - Registra a trilha imutável de transições de status da solicitação.
--   - Não possui soft delete.
--   - Não possui atualização funcional.
--   - Cada alteração de status gera um novo registro.
--   - Na criação da solicitação:
--       status_anterior = NULL
--       status_novo = CRIADA
--   - Em cancelamentos:
--       motivo é obrigatório.
--   - O UUID é gerado pelo Hibernate no fluxo da aplicação.
--     O DEFAULT gen_random_uuid() permanece como fallback no banco.
-- =============================================================

CREATE TABLE historico_status_solicitacao (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    solicitacao_id      UUID            NOT NULL,
    status_anterior     VARCHAR(50)     NULL,
    status_novo         VARCHAR(50)     NOT NULL,
    alterado_por_id     UUID            NOT NULL,
    motivo              VARCHAR(500)    NULL,
    criado_em           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_historico_status_solicitacao PRIMARY KEY (id),

    CONSTRAINT fk_historico_status_solicitacao_solicitacao
        FOREIGN KEY (solicitacao_id)
        REFERENCES solicitacoes_arte (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_historico_status_solicitacao_alterado_por
        FOREIGN KEY (alterado_por_id)
        REFERENCES usuarios (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_historico_status_solicitacao_status_anterior CHECK (
        status_anterior IS NULL
        OR status_anterior IN (
            'CRIADA',
            'EM_PRODUCAO',
            'AGUARDANDO_VALIDACAO_ADMIN',
            'AGUARDANDO_ACEITE_CLIENTE',
            'FINALIZADA',
            'CANCELADA'
        )
    ),

    CONSTRAINT ck_historico_status_solicitacao_status_novo CHECK (
        status_novo IN (
            'CRIADA',
            'EM_PRODUCAO',
            'AGUARDANDO_VALIDACAO_ADMIN',
            'AGUARDANDO_ACEITE_CLIENTE',
            'FINALIZADA',
            'CANCELADA'
        )
    ),

    CONSTRAINT ck_historico_status_solicitacao_transicao_real CHECK (
        status_anterior IS NULL
        OR status_anterior <> status_novo
    ),

    CONSTRAINT ck_historico_status_solicitacao_cancelamento_com_motivo CHECK (
        status_novo <> 'CANCELADA'
        OR (
            motivo IS NOT NULL
            AND LENGTH(BTRIM(motivo)) > 0
        )
    )
);

-- Índice composto para consulta cronológica do histórico de uma solicitação.
CREATE INDEX idx_historico_solicitacao_criado_em
    ON historico_status_solicitacao (solicitacao_id, criado_em ASC);

COMMENT ON TABLE historico_status_solicitacao
IS 'Trilha imutável de transições de status das solicitações de arte';

COMMENT ON COLUMN historico_status_solicitacao.id
IS 'Identificador único UUID do registro de histórico';

COMMENT ON COLUMN historico_status_solicitacao.solicitacao_id
IS 'Solicitação de arte relacionada à transição de status';

COMMENT ON COLUMN historico_status_solicitacao.status_anterior
IS 'Status anterior da solicitação — NULL no registro inicial de criação';

COMMENT ON COLUMN historico_status_solicitacao.status_novo
IS 'Novo status assumido pela solicitação';

COMMENT ON COLUMN historico_status_solicitacao.alterado_por_id
IS 'Usuário responsável pela ação que provocou a transição de status';

COMMENT ON COLUMN historico_status_solicitacao.motivo
IS 'Motivo da transição quando exigido pela regra de negócio, como cancelamento e futuras refações';

COMMENT ON COLUMN historico_status_solicitacao.criado_em
IS 'Timestamp imutável de criação do histórico em UTC';
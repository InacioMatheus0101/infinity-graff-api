-- =============================================================
-- INFINITY GRAFF — Migration V4
-- Criação da tabela de solicitações de arte
-- =============================================================
-- Design:
--   - Representa o núcleo do fluxo operacional da Fase 2.
--   - A solicitação sempre pertence a um CLIENTE.
--   - O prestador é opcional na criação e atribuído posteriormente.
--   - Status inicial da regra de negócio: CRIADA.
--   - cancelado_em representa cancelamento de negócio.
--   - deletado_em representa soft delete técnico/administrativo.
--   - Campos de aprovação e aceite são criados desde já para evitar
--     migration adicional na etapa do painel.
-- =============================================================

CREATE TABLE solicitacoes_arte (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    cliente_id                  UUID            NOT NULL,
    prestador_id                UUID            NULL,

    tipo                        VARCHAR(30)     NOT NULL,
    titulo                      VARCHAR(150)    NOT NULL,
    instrucoes                  TEXT            NOT NULL,
    medidas                     VARCHAR(100)    NULL,
    observacoes_internas        TEXT            NULL,

    status                      VARCHAR(50)     NOT NULL DEFAULT 'CRIADA',

    cancelado_em                TIMESTAMPTZ     NULL,
    motivo_cancelamento         VARCHAR(500)    NULL,

    aprovada_admin_em           TIMESTAMPTZ     NULL,
    aprovada_admin_por_id       UUID            NULL,

    aceita_cliente_em           TIMESTAMPTZ     NULL,
    aceita_cliente_por_id       UUID            NULL,

    criado_em                   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    atualizado_em               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deletado_em                 TIMESTAMPTZ     NULL,

    CONSTRAINT pk_solicitacoes_arte PRIMARY KEY (id),

    CONSTRAINT fk_solicitacoes_arte_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES usuarios (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_solicitacoes_arte_prestador
        FOREIGN KEY (prestador_id)
        REFERENCES usuarios (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_solicitacoes_arte_aprovada_admin_por
        FOREIGN KEY (aprovada_admin_por_id)
        REFERENCES usuarios (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_solicitacoes_arte_aceita_cliente_por
        FOREIGN KEY (aceita_cliente_por_id)
        REFERENCES usuarios (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_solicitacoes_arte_tipo CHECK (
        tipo IN ('RECRIACAO', 'CRIACAO', 'AJUSTE')
    ),

    CONSTRAINT ck_solicitacoes_arte_status CHECK (
        status IN (
            'CRIADA',
            'EM_PRODUCAO',
            'AGUARDANDO_VALIDACAO_ADMIN',
            'AGUARDANDO_ACEITE_CLIENTE',
            'FINALIZADA',
            'CANCELADA'
        )
    ),

    CONSTRAINT ck_solicitacoes_arte_cancelamento_consistente CHECK (
        (
            status <> 'CANCELADA'
            AND cancelado_em IS NULL
            AND motivo_cancelamento IS NULL
        )
        OR
        (
            status = 'CANCELADA'
            AND cancelado_em IS NOT NULL
            AND motivo_cancelamento IS NOT NULL
        )
    )
);

-- Índice para listar solicitações por cliente em registros não deletados.
CREATE INDEX idx_solicitacoes_arte_cliente_id
    ON solicitacoes_arte (cliente_id)
    WHERE deletado_em IS NULL;

-- Índice para listar solicitações atribuídas a prestadores em registros não deletados.
CREATE INDEX idx_solicitacoes_arte_prestador_id
    ON solicitacoes_arte (prestador_id)
    WHERE deletado_em IS NULL
      AND prestador_id IS NOT NULL;

-- Índice para filtros por status em registros não deletados.
CREATE INDEX idx_solicitacoes_arte_status
    ON solicitacoes_arte (status)
    WHERE deletado_em IS NULL;

-- Índice para filtros por tipo em registros não deletados.
CREATE INDEX idx_solicitacoes_arte_tipo
    ON solicitacoes_arte (tipo)
    WHERE deletado_em IS NULL;

-- Índice para ordenação por data de criação em listagens.
CREATE INDEX idx_solicitacoes_arte_criado_em
    ON solicitacoes_arte (criado_em DESC)
    WHERE deletado_em IS NULL;

-- Índice útil para fila de solicitações ainda sem prestador atribuído.
CREATE INDEX idx_solicitacoes_arte_sem_prestador
    ON solicitacoes_arte (criado_em DESC)
    WHERE deletado_em IS NULL
      AND prestador_id IS NULL
      AND status = 'CRIADA';

-- Índice parcial para consultas de registros não deletados.
CREATE INDEX idx_solicitacoes_arte_deletado_em
    ON solicitacoes_arte (deletado_em)
    WHERE deletado_em IS NULL;

COMMENT ON TABLE solicitacoes_arte IS 'Solicitações de arte criadas por clientes ou equipe interna';

COMMENT ON COLUMN solicitacoes_arte.id IS 'Identificador único UUID da solicitação de arte';
COMMENT ON COLUMN solicitacoes_arte.cliente_id IS 'Usuário CLIENTE dono da solicitação';
COMMENT ON COLUMN solicitacoes_arte.prestador_id IS 'Usuário PRESTADOR atribuído posteriormente para executar a solicitação';

COMMENT ON COLUMN solicitacoes_arte.tipo IS 'Tipo da solicitação: RECRIACAO, CRIACAO ou AJUSTE';
COMMENT ON COLUMN solicitacoes_arte.titulo IS 'Título curto da solicitação para identificação em listagens';
COMMENT ON COLUMN solicitacoes_arte.instrucoes IS 'Instruções informadas pelo cliente sobre como deseja a arte';
COMMENT ON COLUMN solicitacoes_arte.medidas IS 'Medidas em formato textual livre, exemplo: 30x40cm, A4 ou 1080x1920px';
COMMENT ON COLUMN solicitacoes_arte.observacoes_internas IS 'Observações internas visíveis apenas para equipe autorizada e prestador vinculado';

COMMENT ON COLUMN solicitacoes_arte.status IS 'Status atual do fluxo operacional da solicitação';
COMMENT ON COLUMN solicitacoes_arte.cancelado_em IS 'Momento do cancelamento de negócio da solicitação';
COMMENT ON COLUMN solicitacoes_arte.motivo_cancelamento IS 'Motivo obrigatório quando a solicitação é cancelada';

COMMENT ON COLUMN solicitacoes_arte.aprovada_admin_em IS 'Momento da aprovação administrativa da arte, usado em etapa futura';
COMMENT ON COLUMN solicitacoes_arte.aprovada_admin_por_id IS 'Usuário ADMIN ou GERENTE que aprovou administrativamente a arte';
COMMENT ON COLUMN solicitacoes_arte.aceita_cliente_em IS 'Momento em que o cliente aceitou a arte, usado em etapa futura';
COMMENT ON COLUMN solicitacoes_arte.aceita_cliente_por_id IS 'Usuário CLIENTE que aceitou a arte';

COMMENT ON COLUMN solicitacoes_arte.criado_em IS 'Timestamp de criação do registro em UTC';
COMMENT ON COLUMN solicitacoes_arte.atualizado_em IS 'Timestamp da última atualização em UTC';
COMMENT ON COLUMN solicitacoes_arte.deletado_em IS 'Timestamp do soft delete técnico — NULL significa não deletado';
-- =============================================================
-- INFINITY GRAFF — Migration V3
-- Adiciona foto de perfil ao usuário
-- =============================================================
-- Design:
--   - A imagem real não é armazenada no banco.
--   - O banco guarda apenas a URL ou path da foto em storage externo.
--   - O campo é opcional, pois o usuário pode não ter foto de perfil.
-- =============================================================

ALTER TABLE usuarios
    ADD COLUMN foto_perfil_url VARCHAR(500) NULL;

COMMENT ON COLUMN usuarios.foto_perfil_url
IS 'URL ou path da foto de perfil do usuário armazenada em serviço externo';